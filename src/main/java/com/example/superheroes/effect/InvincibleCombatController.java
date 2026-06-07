package com.example.superheroes.effect;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.InvincibleHero;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class InvincibleCombatController {
	private static final int PROC_COOLDOWN_TICKS = 8;
	private static final double BONUS_DAMAGE = 4.0;
	private static final double BONUS_KNOCKBACK = 1.15;
	private static final ResourceLocation DAMAGE_PROC = ModId.of("modifiers/invincible/iron_fists_proc_damage");
	private static final ResourceLocation KNOCKBACK_PROC = ModId.of("modifiers/invincible/iron_fists_proc_knockback");
	private static final Map<UUID, Long> LAST_PROC = new HashMap<>();
	private static final Set<UUID> ACTIVE_MODIFIERS = new HashSet<>();

	private InvincibleCombatController() {
	}

	public static void init() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer sp)) {
				return InteractionResult.PASS;
			}
			if (!(entity instanceof LivingEntity target) || target == sp || !target.isAlive()) {
				return InteractionResult.PASS;
			}
			if (target instanceof Player targetPlayer && (targetPlayer.isCreative() || targetPlayer.isSpectator())) {
				return InteractionResult.PASS;
			}
			HeroData data = sp.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (!InvincibleHero.ID.equals(data.heroId()) || data.isActive(AbilityIds.IRON_FISTS)) {
				return InteractionResult.PASS;
			}
			long now = sp.level().getGameTime();
			Long last = LAST_PROC.get(sp.getUUID());
			if (last != null && now - last < PROC_COOLDOWN_TICKS) {
				return InteractionResult.PASS;
			}
			LAST_PROC.put(sp.getUUID(), now);
			applyAttackModifiers(sp);
			spawnImpact(sp.serverLevel(), target);
			return InteractionResult.PASS;
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<UUID> it = ACTIVE_MODIFIERS.iterator();
			while (it.hasNext()) {
				UUID id = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(id);
				if (player != null) {
					removeAttackModifiers(player);
				}
				it.remove();
			}
			LAST_PROC.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
		});
	}

	private static void applyAttackModifiers(ServerPlayer player) {
		AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null) {
			damage.removeModifier(DAMAGE_PROC);
			damage.addTransientModifier(new AttributeModifier(
					DAMAGE_PROC, BONUS_DAMAGE, AttributeModifier.Operation.ADD_VALUE));
		}
		AttributeInstance knockback = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
		if (knockback != null) {
			knockback.removeModifier(KNOCKBACK_PROC);
			knockback.addTransientModifier(new AttributeModifier(
					KNOCKBACK_PROC, BONUS_KNOCKBACK, AttributeModifier.Operation.ADD_VALUE));
		}
		ACTIVE_MODIFIERS.add(player.getUUID());
	}

	private static void removeAttackModifiers(ServerPlayer player) {
		AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null) {
			damage.removeModifier(DAMAGE_PROC);
		}
		AttributeInstance knockback = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
		if (knockback != null) {
			knockback.removeModifier(KNOCKBACK_PROC);
		}
	}

	private static void spawnImpact(ServerLevel level, LivingEntity target) {
		double x = target.getX();
		double y = target.getY() + target.getBbHeight() * 0.55;
		double z = target.getZ();
		level.sendParticles(ParticleTypes.CRIT, x, y, z, 12, 0.25, 0.25, 0.25, 0.16);
		level.sendParticles(ParticleTypes.END_ROD, x, y, z, 8, 0.22, 0.22, 0.22, 0.03);
		level.playSound(null, x, y, z,
				ModSounds.HOMELANDER_IRON_FISTS_IMPACT, SoundSource.PLAYERS, 0.5f, 1.35f);
	}
}
