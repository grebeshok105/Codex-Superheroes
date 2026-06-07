package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.hero.HeroAttributes;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DoomsdayAdaptationController {
	private static final float CUMULATIVE_THRESHOLD = 15f;
	private static final long REALTIME_THRESHOLD_MS = 5_000L;
	private static final long IDLE_RESET_MS = 10_000L;
	private static final float ADAPT_DAMAGE_BONUS = 1.0f;

	/**
	 * Generic damage types — слишком общие, чтобы писать на них постоянный иммун.
	 * Если думсдей умер от player_attack/mob_attack/magic — тир апается, но
	 * иммун не записывается; иначе один удар мечом давал бы иммун ко всем мечам.
	 */
	private static final Set<ResourceKey<DamageType>> GENERIC_TYPES = Set.of(
			DamageTypes.PLAYER_ATTACK,
			DamageTypes.MOB_ATTACK,
			DamageTypes.MOB_ATTACK_NO_AGGRO,
			DamageTypes.ARROW,
			DamageTypes.TRIDENT,
			DamageTypes.GENERIC,
			DamageTypes.GENERIC_KILL,
			DamageTypes.MAGIC,
			DamageTypes.INDIRECT_MAGIC,
			DamageTypes.THROWN,
			DamageTypes.UNATTRIBUTED_FIREBALL,
			DamageTypes.MOB_PROJECTILE,
			DamageTypes.SONIC_BOOM
	);

	/**
	 * Группы родственных damage types — адаптация к любому в группе даёт иммун ко всей группе.
	 * Например адаптация к in_fire блокирует и on_fire, lava, hot_floor, и т.д.
	 */
	private static final java.util.List<Set<ResourceKey<DamageType>>> RELATED_GROUPS = java.util.List.of(
			Set.of(DamageTypes.IN_FIRE, DamageTypes.ON_FIRE, DamageTypes.LAVA, DamageTypes.HOT_FLOOR, DamageTypes.UNATTRIBUTED_FIREBALL, DamageTypes.FIREBALL),
			Set.of(DamageTypes.FREEZE),
			Set.of(DamageTypes.DROWN, DamageTypes.IN_WALL),
			Set.of(DamageTypes.EXPLOSION, DamageTypes.PLAYER_EXPLOSION),
			Set.of(DamageTypes.FALL, DamageTypes.FLY_INTO_WALL, DamageTypes.STALAGMITE, DamageTypes.FALLING_BLOCK, DamageTypes.FALLING_ANVIL, DamageTypes.FALLING_STALACTITE),
			Set.of(DamageTypes.CACTUS, DamageTypes.SWEET_BERRY_BUSH),
			Set.of(DamageTypes.WITHER, DamageTypes.WITHER_SKULL),
			Set.of(DamageTypes.LIGHTNING_BOLT),
			Set.of(
					com.example.superheroes.damage.ModDamageTypes.HOMELANDER_EYE_LASER,
					com.example.superheroes.damage.ModDamageTypes.HOMELANDER_HEAT_VISION,
					com.example.superheroes.damage.ModDamageTypes.HOMELANDER_LIGHTNING_CALL
			),
			Set.of(
					com.example.superheroes.damage.ModDamageTypes.HOMELANDER_HAND_CLAP,
					com.example.superheroes.damage.ModDamageTypes.HOMELANDER_SONIC_SLAM,
					com.example.superheroes.damage.ModDamageTypes.HOMELANDER_SHOCKWAVE_DIVE,
					com.example.superheroes.damage.ModDamageTypes.HOMELANDER_ROAR_BOSS
			)
	);

	private static final Map<UUID, Set<ResourceKey<DamageType>>> ADAPTED = new ConcurrentHashMap<>();
	private static final Map<UUID, Set<ResourceKey<DamageType>>> BANNED = new ConcurrentHashMap<>();
	private static final Map<UUID, Map<ResourceKey<DamageType>, Float>> CUMULATIVE = new ConcurrentHashMap<>();
	private static final Map<UUID, Map<ResourceKey<DamageType>, Long>> FIRST_HIT_MS = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> ADAPT_COUNT = new ConcurrentHashMap<>();

	private DoomsdayAdaptationController() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return true;
			}
			if (!isDoomsday(player)) {
				return true;
			}
			ResourceKey<DamageType> typeKey = source.typeHolder().unwrapKey().orElse(null);
			if (typeKey == null) {
				return true;
			}

			Set<ResourceKey<DamageType>> adapted = ADAPTED.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
			Set<ResourceKey<DamageType>> banned = BANNED.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
			if (adapted.contains(typeKey) || isAdaptedRelated(adapted, typeKey)) {
				return false;
			}

			// Auto-adapt: накапливаем урон по типу; при достижении порога ИЛИ через REALTIME_THRESHOLD_MS — иммун.
			if (!GENERIC_TYPES.contains(typeKey) && !banned.contains(typeKey) && !isInBannedGroup(banned, typeKey)) {
				long now = System.currentTimeMillis();
				Map<ResourceKey<DamageType>, Float> perType = CUMULATIVE.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
				Map<ResourceKey<DamageType>, Long> firstHit = FIRST_HIT_MS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
				Long firstTs = firstHit.get(typeKey);
				if (firstTs == null || now - firstTs > IDLE_RESET_MS) {
					firstHit.put(typeKey, now);
					perType.put(typeKey, amount);
					firstTs = now;
				} else {
					perType.merge(typeKey, amount, Float::sum);
				}
				float total = perType.getOrDefault(typeKey, 0f);
				boolean reachedDamage = total >= CUMULATIVE_THRESHOLD;
				boolean reachedTime = (now - firstTs) >= REALTIME_THRESHOLD_MS;
				if (reachedDamage || reachedTime) {
					registerAdaptation(player, typeKey, false);
					perType.remove(typeKey);
					firstHit.remove(typeKey);
				}
			}

			return true;
		});
	}

	private static boolean isAdaptedRelated(Set<ResourceKey<DamageType>> adapted, ResourceKey<DamageType> typeKey) {
		for (Set<ResourceKey<DamageType>> group : RELATED_GROUPS) {
			if (group.contains(typeKey)) {
				for (ResourceKey<DamageType> related : group) {
					if (adapted.contains(related)) return true;
				}
			}
		}
		return false;
	}

	private static boolean isInBannedGroup(Set<ResourceKey<DamageType>> banned, ResourceKey<DamageType> typeKey) {
		for (Set<ResourceKey<DamageType>> group : RELATED_GROUPS) {
			if (group.contains(typeKey)) {
				for (ResourceKey<DamageType> related : group) {
					if (banned.contains(related)) return true;
				}
			}
		}
		return false;
	}

	public static boolean wouldBlock(ServerPlayer player, DamageSource source) {
		ResourceKey<DamageType> typeKey = source.typeHolder().unwrapKey().orElse(null);
		if (typeKey == null) return false;
		Set<ResourceKey<DamageType>> adapted = ADAPTED.get(player.getUUID());
		if (adapted == null) return false;
		if (adapted.contains(typeKey)) return true;
		return isAdaptedRelated(adapted, typeKey);
	}

	public static ResourceKey<DamageType> stripOneAdaptation(ServerPlayer doomsday) {
		Set<ResourceKey<DamageType>> adapted = ADAPTED.get(doomsday.getUUID());
		if (adapted == null || adapted.isEmpty()) return null;
		ResourceKey<DamageType> picked = adapted.iterator().next();
		adapted.remove(picked);
		Set<ResourceKey<DamageType>> banned = BANNED.computeIfAbsent(doomsday.getUUID(), k -> new HashSet<>());
		banned.add(picked);
		Integer count = ADAPT_COUNT.get(doomsday.getUUID());
		if (count != null && count > 0) {
			int next = count - 1;
			if (next <= 0) {
				ADAPT_COUNT.remove(doomsday.getUUID());
				AttributeInstance inst = doomsday.getAttribute(Attributes.ATTACK_DAMAGE);
				if (inst != null) inst.removeModifier(HeroAttributes.DOOMSDAY_ADAPT_DAMAGE);
			} else {
				ADAPT_COUNT.put(doomsday.getUUID(), next);
				applyDamageBonus(doomsday, next);
			}
		}
		return picked;
	}

	public static void registerAdaptation(ServerPlayer player, ResourceKey<DamageType> typeKey, boolean lethal) {
		if (GENERIC_TYPES.contains(typeKey)) {
			return;
		}
		Set<ResourceKey<DamageType>> adapted = ADAPTED.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
		if (!adapted.add(typeKey)) {
			return;
		}
		int count = ADAPT_COUNT.merge(player.getUUID(), 1, Integer::sum);
		applyDamageBonus(player, count);

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.4f, 0.5f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				com.example.superheroes.sound.ModSounds.DOOMSDAY_ROAR, SoundSource.PLAYERS, 0.5f, 0.95f);

		String typePath = typeKey.location().getPath();
		Component title = Component.translatable("hero.superheroes.doomsday.adapted",
				Component.literal(typePath).withStyle(ChatFormatting.RED))
				.withStyle(lethal ? ChatFormatting.DARK_RED : ChatFormatting.GOLD);
		player.displayClientMessage(title, true);
		player.sendSystemMessage(Component.translatable("hero.superheroes.doomsday.adapted.chat",
				Component.literal(typePath).withStyle(ChatFormatting.LIGHT_PURPLE),
				Component.literal(String.valueOf(count)).withStyle(ChatFormatting.RED))
				.withStyle(ChatFormatting.GRAY));
	}

	private static void applyDamageBonus(ServerPlayer player, int count) {
		AttributeInstance inst = player.getAttribute(Attributes.ATTACK_DAMAGE);
		if (inst == null) return;
		double amount = ADAPT_DAMAGE_BONUS * count;
		inst.addOrReplacePermanentModifier(new AttributeModifier(
				HeroAttributes.DOOMSDAY_ADAPT_DAMAGE, amount, AttributeModifier.Operation.ADD_VALUE));
	}

	/** Вызывать на респавне — attribute modifier сбрасывается при remove/apply набора. */
	public static void reapplyDamageBonus(ServerPlayer player) {
		int count = ADAPT_COUNT.getOrDefault(player.getUUID(), 0);
		if (count > 0) {
			applyDamageBonus(player, count);
		}
	}

	public static int getAdaptationCount(ServerPlayer player) {
		return ADAPT_COUNT.getOrDefault(player.getUUID(), 0);
	}

	public static boolean hasAdapted(ServerPlayer player, DamageSource source) {
		ResourceKey<DamageType> typeKey = source.typeHolder().unwrapKey().orElse(null);
		if (typeKey == null) return false;
		Set<ResourceKey<DamageType>> adapted = ADAPTED.get(player.getUUID());
		return adapted != null && adapted.contains(typeKey);
	}

	public static void clear(ServerPlayer player) {
		UUID id = player.getUUID();
		ADAPTED.remove(id);
		BANNED.remove(id);
		CUMULATIVE.remove(id);
		FIRST_HIT_MS.remove(id);
		ADAPT_COUNT.remove(id);
		AttributeInstance inst = player.getAttribute(Attributes.ATTACK_DAMAGE);
		if (inst != null) {
			inst.removeModifier(HeroAttributes.DOOMSDAY_ADAPT_DAMAGE);
		}
	}

	private static boolean isDoomsday(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && DoomsdayHero.ID.equals(data.heroId());
	}
}
