package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.attachment.SungShadowArmy;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.entity.ModEntities;
import io.github.grebeshok105.codex.entity.ShadowSoldierEntity;
import io.github.grebeshok105.codex.hero.SungJinwooHero;
import io.github.grebeshok105.codex.network.SungShadowArmyS2CPayload;
import io.github.grebeshok105.codex.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;

/**
 * Управляет армией Теневых Солдат для Сон Джи Ву.
 *
 *  - При активации героя один раз спавнится 10 теней (5 наземных + 5 летающих)
 *    в полукруге сзади.
 *  - **Тени одноразовые** — после смерти не возвращаются. Новые тени можно
 *    получить только через Arise (поднять из тела врага).
 *  - ВЕСЬ урон по Сону всегда переадресуется на одну случайную живую тень
 *    (без радиуса). Если живых нет — Сон получает урон сам.
 *  - При снятии костюма / выходе из игры — тени деспавнятся.
 *  - Армия хранится в persistent-attachment {@link SungShadowArmy} (audit B18):
 *    после рестарта сервера сохранённые тени перелинковываются по UUID, а не
 *    спавнятся заново. Тень-сирота (владелец снял героя / ушёл) не атакует и
 *    удаляет себя, когда владелец онлайн и её нет в его армии.
 */
public final class SungJinwooController {
	public static final int MAX_SHADOWS = 10;

	private static final Map<ServerLevel, List<DeathEcho>> DEATH_ECHOES = new ConcurrentHashMap<>();
	private static final Set<UUID> SUPPRESSED_DEATH_ECHOES = ConcurrentHashMap.newKeySet();
	private static final Random RNG = new Random();
	/** TTL для буфера смертей: 30 минут (фактически не теряем эхо до Arise). */
	private static final long DEATH_ECHO_TICKS = 20L * 60L * 30L;
	/** Радиус сбора эхо при Arise (см. §1). */
	public static final double ARISE_RANGE = 50.0;

	private SungJinwooController() {
	}

	public static void register(HeroModuleContext ctx) {

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player)) return true;
			if (!isSung(player)) return true;
			// /kill, урон от пустоты и прочие источники, игнорирующие неуязвимость,
			// не отражаются на тени — иначе Sung неубиваем, пока жива хоть одна.
			if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			ShadowSoldierEntity victim = pickRandomAliveShadow(player);
			if (victim == null) return true;
			Entity src = source.getEntity();
			if (src != null && src.getUUID().equals(victim.getUUID())) return true;
			DamageSource diverted = source;
			if (src != null && src.getUUID().equals(player.getUUID())) {
				diverted = player.damageSources().generic();
			}
			victim.hurt(diverted, amount);
			ServerLevel level = player.serverLevel();
			level.sendParticles(ParticleTypes.WARPED_SPORE, player.getX(), player.getY() + 1.0, player.getZ(),
					16, 0.4, 0.6, 0.4, 0.05);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.6f, 0.8f);
			return false;
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity.level() instanceof ServerLevel level)) return;
			if (entity instanceof Player || entity instanceof ShadowSoldierEntity) return;
			if (SUPPRESSED_DEATH_ECHOES.remove(entity.getUUID())) return;
			List<DeathEcho> list = DEATH_ECHOES.computeIfAbsent(level, l -> new ArrayList<>());
			long now = level.getGameTime();
			list.removeIf(e -> e.expiresAt() <= now);
			list.add(new DeathEcho(entity.position(), now + DEATH_ECHO_TICKS));
		});

		ctx.lifecycle().onServerStopped(server -> SungJinwooController.resetAll());
	}

	public static boolean isSung(ServerPlayer player) {
		HeroData data = player.getAttached(ModAttachments.HERO_DATA);
		return data != null && SungJinwooHero.ID.equals(data.heroId());
	}

	private static SungShadowArmy army(ServerPlayer player) {
		SungShadowArmy a = player.getAttached(ModAttachments.SUNG_SHADOW_ARMY);
		return a == null ? SungShadowArmy.EMPTY : a;
	}

	private static void saveArmy(ServerPlayer player, SungShadowArmy army) {
		player.setAttached(ModAttachments.SUNG_SHADOW_ARMY, army);
	}

	/** Legitimacy check a shadow entity can run on itself: owner is Sung and lists it. */
	public static boolean isArmyMember(ServerPlayer owner, UUID shadowId) {
		return isSung(owner) && army(owner).shadowIds().contains(shadowId);
	}

	private static void tickPlayer(ServerPlayer player) {
		boolean sung = isSung(player);
		if (!sung) {
			disbandIfPresent(player);
			return;
		}

		SungShadowArmy a = army(player);
		List<UUID> ids = new ArrayList<>(a.shadowIds());
		ServerLevel level = player.serverLevel();
		// Чистим мёртвых — но НЕ респавним. Неразрешённый UUID (невыгруженный
		// чанк) остаётся в списке: тень перелинкуется, когда чанк загрузится.
		boolean changed = ids.removeIf(uuid -> {
			Entity e = level.getEntity(uuid);
			return e != null && (!(e instanceof ShadowSoldierEntity ss) || !ss.isAlive());
		});
		if (!a.summoned()) {
			summonInitialArmy(player);
			a = army(player);
			ids = new ArrayList<>(a.shadowIds());
		} else if (changed) {
			saveArmy(player, a.withShadows(ids));
		}

		// Periodic resend so a player who just walked into tracking range
		// converges without waiting for the next count change.
		if (player.tickCount % 100 == 0) {
			LAST_SENT.remove(player.getUUID());
		}
		broadcastArmyState(player, !ids.isEmpty(), ids.size());
	}

	public static void enterPhase2(ServerPlayer player) {
		if (!isSung(player)) return;
		SungShadowArmy a = army(player);
		if (a.phase2()) return;
		saveArmy(player, a.withPhase2(true));
		List<UUID> ids = a.shadowIds();
		broadcastArmyState(player, !ids.isEmpty(), ids.size());
	}

	public static boolean isPhase2(ServerPlayer player) {
		return army(player).phase2();
	}

	public static void resetPhase(ServerPlayer player) {
		SungShadowArmy a = army(player);
		if (a.phase2()) {
			saveArmy(player, a.withPhase2(false));
		}
	}

	/** World shutdown — level-keyed echo state must not leak into a new world. */
	public static void resetAll() {
		DEATH_ECHOES.clear();
		SUPPRESSED_DEATH_ECHOES.clear();
		LAST_SENT.clear();
	}

	public static void summonInitialArmy(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		List<UUID> ids = new ArrayList<>();
		// 5 наземных, 5 летающих, расставлены полукругом сзади
		for (int i = 0; i < MAX_SHADOWS; i++) {
			boolean grounded = (i % 2 == 0); // чередуем
			ShadowSoldierEntity shadow = spawnSlotShadow(level, player, i, MAX_SHADOWS, grounded);
			if (shadow != null) {
				ids.add(shadow.getUUID());
			}
		}
		saveArmy(player, army(player).withShadows(ids).withSummoned(true));
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 0.5f, 1.6f);
		level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
				120, 1.5, 1.5, 1.5, 0.4);
	}

	private static ShadowSoldierEntity spawnSlotShadow(ServerLevel level, ServerPlayer owner,
			int slotIndex, int slotCount, boolean grounded) {
		double radius = grounded ? 3.0 : 4.0;
		// Полукруг за спиной: yaw + 180 ± 90°
		double baseYawRad = Math.toRadians(owner.getYRot() + 180.0);
		double slotAngle = (slotCount > 1)
				? (slotIndex - (slotCount - 1) / 2.0) * (Math.PI / Math.max(1, slotCount - 1)) * 0.95
				: 0.0;
		double angle = baseYawRad + slotAngle;
		double dx = -Math.sin(angle) * radius;
		double dz = Math.cos(angle) * radius;
		double y = grounded ? owner.getY() : owner.getY() + 2.5;
		Vec3 pos = new Vec3(owner.getX() + dx, y, owner.getZ() + dz);
		return spawnConfiguredShadowAt(level, owner, pos, slotIndex, slotCount, grounded);
	}

	public static ShadowSoldierEntity spawnConfiguredShadowAt(ServerLevel level, ServerPlayer owner,
			Vec3 pos, int slotIndex, int slotCount, boolean grounded) {
		ShadowSoldierEntity shadow = ModEntities.SHADOW_SOLDIER.create(level);
		if (shadow == null) return null;
		shadow.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0f);
		shadow.setOwnerId(owner.getUUID());
		shadow.setVariant(RNG.nextInt(ShadowSoldierEntity.VARIANT_COUNT));
		shadow.setGrounded(grounded);
		shadow.setSlot(slotIndex, slotCount);
		shadow.finalizeSpawn(level, level.getCurrentDifficultyAt(shadow.blockPosition()),
				MobSpawnType.MOB_SUMMONED, null);
		level.addFreshEntity(shadow);
		level.sendParticles(ParticleTypes.SOUL, pos.x, pos.y + 0.5, pos.z, 18, 0.3, 0.4, 0.3, 0.05);
		return shadow;
	}

	/** Совместимость с прежним API (Arise и др.). Случайный слот, случайная позиция. */
	public static ShadowSoldierEntity spawnOneShadowAt(ServerLevel level, ServerPlayer owner, Vec3 pos) {
		boolean grounded = RNG.nextBoolean();
		int slotIndex = army(owner).shadowIds().size();
		int slotCount = Math.max(MAX_SHADOWS, slotIndex + 1);
		return spawnConfiguredShadowAt(level, owner, pos, slotIndex, slotCount, grounded);
	}

	public static int aliveCount(ServerPlayer player) {
		int n = 0;
		for (UUID id : army(player).shadowIds()) {
			Entity e = player.serverLevel().getEntity(id);
			if (e instanceof ShadowSoldierEntity ss && ss.isAlive()) n++;
		}
		return n;
	}

	public static List<ShadowSoldierEntity> aliveShadows(ServerPlayer player) {
		List<ShadowSoldierEntity> out = new ArrayList<>();
		for (UUID id : army(player).shadowIds()) {
			Entity e = player.serverLevel().getEntity(id);
			if (e instanceof ShadowSoldierEntity ss && ss.isAlive()) out.add(ss);
		}
		return out;
	}

	public static ShadowSoldierEntity pickRandomAliveShadow(ServerPlayer player) {
		List<ShadowSoldierEntity> list = aliveShadows(player);
		if (list.isEmpty()) return null;
		return list.get(RNG.nextInt(list.size()));
	}

	public static Vec3 nearestDeathEcho(ServerPlayer player, double range) {
		List<DeathEcho> list = DEATH_ECHOES.get(player.serverLevel());
		if (list == null) return null;
		long now = player.serverLevel().getGameTime();
		list.removeIf(e -> e.expiresAt() <= now);
		double maxDistance = range * range;
		return list.stream()
				.filter(e -> e.pos().distanceToSqr(player.position()) <= maxDistance)
				.min(Comparator.comparingDouble(e -> e.pos().distanceToSqr(player.position())))
				.map(DeathEcho::pos)
				.orElse(null);
	}

	/**
	 * §1: вытащить и удалить ВСЕ эхо смертей в радиусе {@code range} от Сона.
	 * Используется Arise для одномоментного подъёма всех мертвых в зоне.
	 */
	public static List<Vec3> drainDeathEchoesInRange(ServerPlayer player, double range) {
		List<DeathEcho> list = DEATH_ECHOES.get(player.serverLevel());
		List<Vec3> drained = new ArrayList<>();
		if (list == null || list.isEmpty()) return drained;
		long now = player.serverLevel().getGameTime();
		list.removeIf(e -> e.expiresAt() <= now);
		double rSq = range * range;
		Vec3 origin = player.position();
		list.removeIf(e -> {
			if (e.pos().distanceToSqr(origin) <= rSq) {
				drained.add(e.pos());
				return true;
			}
			return false;
		});
		return drained;
	}

	public static int countDeathEchoesInRange(ServerPlayer player, double range) {
		List<DeathEcho> list = DEATH_ECHOES.get(player.serverLevel());
		if (list == null || list.isEmpty()) return 0;
		long now = player.serverLevel().getGameTime();
		list.removeIf(e -> e.expiresAt() <= now);
		double rSq = range * range;
		Vec3 origin = player.position();
		int c = 0;
		for (DeathEcho e : list) {
			if (e.pos().distanceToSqr(origin) <= rSq) c++;
		}
		return c;
	}

	public static void consumeDeathEcho(ServerPlayer player, Vec3 pos) {
		List<DeathEcho> list = DEATH_ECHOES.get(player.serverLevel());
		if (list == null) return;
		list.removeIf(e -> e.pos().distanceToSqr(pos) < 0.01);
	}

	public static void suppressDeathEcho(LivingEntity entity) {
		SUPPRESSED_DEATH_ECHOES.add(entity.getUUID());
	}

	public static void registerExtraShadow(ServerPlayer owner, ShadowSoldierEntity shadow) {
		SungShadowArmy a = army(owner);
		List<UUID> ids = new ArrayList<>(a.shadowIds());
		ids.add(shadow.getUUID());
		saveArmy(owner, a.withShadows(ids));
	}

	public static void disbandAll(ServerPlayer player) {
		SungShadowArmy a = army(player);
		saveArmy(player, SungShadowArmy.EMPTY);
		ServerLevel level = player.serverLevel();
		for (UUID id : a.shadowIds()) {
			Entity e = level.getEntity(id);
			if (e instanceof ShadowSoldierEntity ss) {
				level.sendParticles(ParticleTypes.PORTAL, ss.getX(), ss.getY() + 1, ss.getZ(),
						20, 0.3, 0.6, 0.3, 0.15);
				ss.discard();
			}
			// Невыгруженные тени удаляют себя сами при загрузке чанка:
			// их UUID больше нет в армии владельца (см. ShadowSoldierEntity.aiStep).
		}
	}

	private static void disbandIfPresent(ServerPlayer player) {
		SungShadowArmy a = army(player);
		if (!a.shadowIds().isEmpty() || a.summoned() || a.phase2()) {
			disbandAll(player);
			LAST_SENT.remove(player.getUUID()); // force the all-clear send
			broadcastArmyState(player, false, 0);
		}
	}

	/** Last army state sent per owner — broadcast only on change (audit §3). */
	private static final Map<UUID, ArmyState> LAST_SENT = new ConcurrentHashMap<>();

	private record ArmyState(boolean hasShadows, int count, boolean phase2) {
	}

	private static void broadcastArmyState(ServerPlayer player, boolean hasShadows, int count) {
		ArmyState state = new ArmyState(hasShadows, count, army(player).phase2());
		if (state.equals(LAST_SENT.put(player.getUUID(), state))) {
			return;
		}
		SungShadowArmyS2CPayload payload = new SungShadowArmyS2CPayload(
				player.getUUID(), state.hasShadows(), state.count(), state.phase2());
		ServerPlayNetworking.send(player, payload);
		// Only observers who can actually see the owner need the army counter.
		for (ServerPlayer observer : net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(player)) {
			if (observer != player) {
				ServerPlayNetworking.send(observer, payload);
			}
		}
	}

	private record DeathEcho(Vec3 pos, long expiresAt) {
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		tickPlayer(player);
	}

}
