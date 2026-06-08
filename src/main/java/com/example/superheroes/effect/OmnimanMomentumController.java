package com.example.superheroes.effect;

import com.example.superheroes.ModId;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.OmnimanHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OmnimanMomentumController {
	private static final float MAX_MOMENTUM = 100f;
	private static final float IDLE_DECAY = 0.28f;
	private static final float GROUND_MOVEMENT_GAIN = 0.35f;
	private static final float AIR_MOVEMENT_GAIN = 0.7f;
	private static final float FLIGHT_GAIN = 0.55f;
	private static final double MOVING_SPEED_THRESHOLD = 0.055;
	private static final float MIN_ATTACK_MOMENTUM = 10f;
	private static final double MAX_BONUS_DAMAGE = 6.0;
	private static final double MAX_BONUS_KNOCKBACK = 1.35;
	private static final ResourceLocation DAMAGE_PROC = ModId.of("modifiers/omniman/momentum_proc_damage");
	private static final ResourceLocation KNOCKBACK_PROC = ModId.of("modifiers/omniman/momentum_proc_knockback");
	private static final Map<UUID, Float> MOMENTUM = new HashMap<>();
	private static final Set<UUID> ACTIVE_MODIFIERS = new HashSet<>();

	private OmnimanMomentumController() {
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
			if (!isOmniman(sp)) {
				return InteractionResult.PASS;
			}
			float stored = momentum(sp);
			if (stored < MIN_ATTACK_MOMENTUM) {
				return InteractionResult.PASS;
			}
			applyAttackModifiers(sp, stored / MAX_MOMENTUM);
			return InteractionResult.PASS;
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<UUID> activeIt = ACTIVE_MODIFIERS.iterator();
			while (activeIt.hasNext()) {
				UUID id = activeIt.next();
				ServerPlayer player = server.getPlayerList().getPlayer(id);
				if (player != null) {
					removeAttackModifiers(player);
				}
				activeIt.remove();
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
			MOMENTUM.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
		});
	}

	public static float momentum(ServerPlayer player) {
		return MOMENTUM.getOrDefault(player.getUUID(), 0f);
	}

	public static boolean consume(ServerPlayer player, float amount) {
		if (amount <= 0f) {
			return true;
		}
		float current = momentum(player);
		if (current < amount) {
			return false;
		}
		set(player, current - amount);
		return true;
	}

	public static void add(ServerPlayer player, float amount) {
		if (amount <= 0f) {
			return;
		}
		set(player, momentum(player) + amount);
	}

	public static void clear(ServerPlayer player) {
		MOMENTUM.remove(player.getUUID());
		removeAttackModifiers(player);
		ACTIVE_MODIFIERS.remove(player.getUUID());
	}

	private static void tickPlayer(ServerPlayer player) {
		if (!isOmniman(player) || !player.isAlive()) {
			clear(player);
			return;
		}

		Vec3 motion = player.getDeltaMovement();
		double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		double speed = Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);
		boolean airborne = !player.onGround();
		boolean flying = player.getAbilities().flying || player.isFallFlying();
		boolean moving = horizontal > MOVING_SPEED_THRESHOLD || Math.abs(motion.y) > MOVING_SPEED_THRESHOLD;

		if (moving) {
			float gain = (float) Math.min(3.0, speed * (airborne ? AIR_MOVEMENT_GAIN : GROUND_MOVEMENT_GAIN));
			if (airborne) {
				gain += 0.08f;
			}
			if (flying) {
				gain += FLIGHT_GAIN;
			}
			add(player, gain);
			return;
		}

		set(player, momentum(player) - IDLE_DECAY);
	}

	private static boolean isOmniman(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && OmnimanHero.ID.equals(data.heroId());
	}

	private static void applyAttackModifiers(ServerPlayer player, float momentumFraction) {
		double clamped = Math.max(0.0, Math.min(1.0, momentumFraction));
		double damageBonus = MAX_BONUS_DAMAGE * clamped;
		double knockbackBonus = MAX_BONUS_KNOCKBACK * clamped;

		AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null) {
			damage.removeModifier(DAMAGE_PROC);
			damage.addTransientModifier(new AttributeModifier(
					DAMAGE_PROC, damageBonus, AttributeModifier.Operation.ADD_VALUE));
		}
		AttributeInstance knockback = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
		if (knockback != null) {
			knockback.removeModifier(KNOCKBACK_PROC);
			knockback.addTransientModifier(new AttributeModifier(
					KNOCKBACK_PROC, knockbackBonus, AttributeModifier.Operation.ADD_VALUE));
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

	private static void set(ServerPlayer player, float value) {
		float clamped = Math.max(0f, Math.min(MAX_MOMENTUM, value));
		if (clamped <= 0f) {
			MOMENTUM.remove(player.getUUID());
			return;
		}
		MOMENTUM.put(player.getUUID(), clamped);
	}
}
