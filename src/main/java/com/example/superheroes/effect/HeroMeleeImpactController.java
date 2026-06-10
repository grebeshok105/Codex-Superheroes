package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.network.HeroMeleeChargeC2SPayload;
import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.physics.CombatImpactEngine;
import com.example.superheroes.physics.ImpactChargeRules;
import com.example.superheroes.physics.ImpactProfile;
import com.example.superheroes.physics.ImpactTier;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Заряженные удары героев.
 *
 * ЛКМ — обычная ванильная атака (PASS) с лёгким tier-1 эффектом по стилю героя.
 * Зажатая ПКМ (с пустой «боевой» рукой) копит заряд; отпускание на Tier 2/3 наносит
 * заряженный удар по цели на дистанции обычной атаки.
 */
public final class HeroMeleeImpactController {
	private static final double RELEASE_HIT_INFLATE = 0.3;
	private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();
	private static final List<PendingPush> PENDING_PUSHES = new ArrayList<>();

	private HeroMeleeImpactController() {
	}

	public static void init() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer attacker)) {
				return InteractionResult.PASS;
			}
			if (!(entity instanceof LivingEntity target) || !validTarget(attacker, target)) {
				return InteractionResult.PASS;
			}
			HeroData data = attacker.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (!data.hasHero()) {
				return InteractionResult.PASS;
			}
			if (!attacker.getMainHandItem().isEmpty()) {
				// Тиры и их эффекты доступны только с пустой рукой: с предметом — чистая ваниль.
				return InteractionResult.PASS;
			}
			spawnTierOneHitFx(attacker, data, target);
			PENDING_PUSHES.add(new PendingPush(attacker, target,
					CombatImpactEngine.heroPowerOf(data.heroId())));
			return InteractionResult.PASS;
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			processPendingPushes();
			cleanup(server);
		});
	}

	public static void handleChargeInput(ServerPlayer player, HeroMeleeChargeC2SPayload payload) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			CHARGES.remove(player.getUUID());
			return;
		}
		int action = payload.action();
		if (action == HeroMeleeChargeC2SPayload.ACTION_START) {
			if (!player.getMainHandItem().isEmpty()) {
				return;
			}
			CHARGES.putIfAbsent(player.getUUID(), new ChargeState(player.level().getGameTime()));
		} else if (action == HeroMeleeChargeC2SPayload.ACTION_RELEASE
				|| action == HeroMeleeChargeC2SPayload.ACTION_CANCEL) {
			if (action == HeroMeleeChargeC2SPayload.ACTION_RELEASE) {
				releaseChargedAttack(player, data, payload);
			}
			CHARGES.remove(player.getUUID());
		}
	}

	private static void releaseChargedAttack(ServerPlayer player, HeroData data, HeroMeleeChargeC2SPayload payload) {
		if (!player.getMainHandItem().isEmpty()) {
			return;
		}
		int heldTicks = Math.min(heldTicks(player), ImpactChargeRules.cappedTicks(payload.heldTicks()));
		if (ImpactChargeRules.tierFor(heldTicks) == ImpactTier.TIER_1) {
			return;
		}
		double range = meleeRange(player);
		LivingEntity target = findReleaseTarget(player, payload.targetId(), range);
		if (target == null) {
			spawnWhiffFx(player);
			return;
		}
		ImpactProfile profile = CombatImpactEngine.profileFor(player, data.heroId(), target, heldTicks);
		CombatImpactEngine.applyMeleeImpact(player, target, profile);
	}

	private static double meleeRange(ServerPlayer player) {
		return player.entityInteractionRange();
	}

	private static int heldTicks(ServerPlayer player) {
		ChargeState state = CHARGES.get(player.getUUID());
		if (state == null) {
			return 0;
		}
		long held = player.level().getGameTime() - state.startedAt;
		return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, held));
	}

	private static boolean validTarget(ServerPlayer attacker, LivingEntity target) {
		if (target == attacker || !target.isAlive() || target.isSpectator()) {
			return false;
		}
		return !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
	}

	private static LivingEntity findReleaseTarget(ServerPlayer player, int targetId, double range) {
		if (targetId >= 0) {
			Entity entity = player.serverLevel().getEntity(targetId);
			if (entity instanceof LivingEntity target && validTarget(player, target)
					&& target.distanceTo(player) <= range + RELEASE_HIT_INFLATE + 0.5) {
				return target;
			}
		}
		Vec3 eye = player.getEyePosition();
		Vec3 direction = player.getViewVector(1f).normalize();
		if (direction.lengthSqr() < 1.0e-4) {
			return null;
		}
		Vec3 end = eye.add(direction.scale(range));
		BlockHitResult blockHit = player.serverLevel().clip(new ClipContext(
				eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 entityEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : end;
		AABB box = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(RELEASE_HIT_INFLATE);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.serverLevel(), player, eye, entityEnd, box,
				entity -> entity instanceof LivingEntity target && validTarget(player, target));
		if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
			return null;
		}
		return target;
	}

	private static void processPendingPushes() {
		if (PENDING_PUSHES.isEmpty()) {
			return;
		}
		for (PendingPush push : PENDING_PUSHES) {
			LivingEntity target = push.target();
			ServerPlayer attacker = push.attacker();
			if (target == null || !target.isAlive() || attacker == null || attacker.isRemoved()
					|| target.level() != attacker.level()) {
				continue;
			}
			Vec3 away = target.position().subtract(attacker.position());
			Vec3 direction = away.lengthSqr() > 1.0e-4
					? new Vec3(away.x, 0.0, away.z).normalize()
					: new Vec3(attacker.getViewVector(1f).x, 0.0, attacker.getViewVector(1f).z).normalize();
			Vec3 motion = target.getDeltaMovement().add(direction.scale(0.32 * push.heroPower()));
			target.setDeltaMovement(motion.x, Math.max(motion.y, 0.12), motion.z);
			target.hurtMarked = true;
		}
		PENDING_PUSHES.clear();
	}

	private static void cleanup(MinecraftServer server) {
		Iterator<Map.Entry<UUID, ChargeState>> it = CHARGES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, ChargeState> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (!data.hasHero()) {
				it.remove();
				continue;
			}
			tickChargeFeedback(player, data, heldTicks(player));
		}
	}

	private static void tickChargeFeedback(ServerPlayer player, HeroData data, int heldTicks) {
		if (heldTicks <= 0) {
			return;
		}
		ServerLevel level = player.serverLevel();
		ImpactTier tier = ImpactChargeRules.tierFor(heldTicks);
		if (heldTicks == ImpactChargeRules.TIER_2_TICKS || heldTicks == ImpactChargeRules.TIER_3_TICKS) {
			boolean max = heldTicks == ImpactChargeRules.TIER_3_TICKS;
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					max ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.NOTE_BLOCK_BELL.value(),
					SoundSource.PLAYERS, 0.9f, max ? 0.6f : 1.3f);
		}
		if (heldTicks % 5 != 0) {
			return;
		}
		Vec3 view = player.getViewVector(1f).normalize();
		Vec3 fist = player.getEyePosition().add(view.scale(0.65)).add(0.0, -0.35, 0.0);
		int count = tier == ImpactTier.TIER_3 ? 6 : tier == ImpactTier.TIER_2 ? 4 : 2;
		ParticleOptions style = styleParticle(data);
		level.sendParticles(style, fist.x, fist.y, fist.z, count, 0.18, 0.18, 0.18, 0.03);
		if (tier == ImpactTier.TIER_3) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, fist.x, fist.y, fist.z, 2, 0.1, 0.1, 0.1, 0.02);
		}
	}

	private static void spawnTierOneHitFx(ServerPlayer attacker, HeroData data, LivingEntity target) {
		ServerLevel level = attacker.serverLevel();
		Vec3 c = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
		level.sendParticles(styleParticle(data), c.x, c.y, c.z, 8, 0.32, 0.28, 0.32, 0.1);
		level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 4, 0.3, 0.25, 0.3, 0.15);
		ServerPlayNetworking.send(attacker, new ScreenShakeS2CPayload(0.25f, 6));
		if (target instanceof ServerPlayer victim) {
			ServerPlayNetworking.send(victim, new ScreenShakeS2CPayload(0.3f, 6));
		}
	}

	private static void spawnWhiffFx(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 view = player.getViewVector(1f).normalize();
		Vec3 front = player.getEyePosition().add(view.scale(1.2));
		player.swing(InteractionHand.MAIN_HAND, true);
		level.sendParticles(ParticleTypes.POOF, front.x, front.y, front.z, 8, 0.3, 0.3, 0.3, 0.04);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
	}

	private static ParticleOptions styleParticle(HeroData data) {
		return switch (CombatImpactEngine.styleOf(data.heroId())) {
			case SPEED -> ParticleTypes.CLOUD;
			case ENERGY -> ParticleTypes.ELECTRIC_SPARK;
			case WEAPON -> ParticleTypes.ENCHANTED_HIT;
			default -> ParticleTypes.CRIT;
		};
	}

	private record PendingPush(ServerPlayer attacker, LivingEntity target, double heroPower) {
	}

	private static final class ChargeState {
		private final long startedAt;

		private ChargeState(long startedAt) {
			this.startedAt = startedAt;
		}
	}
}
