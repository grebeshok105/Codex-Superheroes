package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.network.HeroMeleeChargeC2SPayload;
import com.example.superheroes.physics.CombatImpactEngine;
import com.example.superheroes.physics.ImpactProfile;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class HeroMeleeImpactController {
	private static final double RELEASE_RANGE = 7.0;
	private static final double RELEASE_HIT_INFLATE = 1.35;
	private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();

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
			if (isCharging(attacker)) {
				return InteractionResult.FAIL;
			}

			int heldTicks = heldTicks(attacker);
			ImpactProfile profile = CombatImpactEngine.profileFor(attacker, data.heroId(), target, heldTicks);
			boolean applied = CombatImpactEngine.applyMeleeImpact(attacker, target, profile);
			if (applied) {
				resetAfterImpact(attacker);
			}
			return InteractionResult.SUCCESS;
		});

		ServerTickEvents.END_SERVER_TICK.register(HeroMeleeImpactController::cleanup);
	}

	public static void handleChargeInput(ServerPlayer player, int action) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			CHARGES.remove(player.getUUID());
			return;
		}
		if (action == HeroMeleeChargeC2SPayload.ACTION_START) {
			CHARGES.putIfAbsent(player.getUUID(), new ChargeState(player.level().getGameTime()));
		} else if (action == HeroMeleeChargeC2SPayload.ACTION_RELEASE
				|| action == HeroMeleeChargeC2SPayload.ACTION_CANCEL) {
			if (action == HeroMeleeChargeC2SPayload.ACTION_RELEASE) {
				releaseChargedAttack(player, data);
			}
			CHARGES.remove(player.getUUID());
		}
	}

	private static void releaseChargedAttack(ServerPlayer player, HeroData data) {
		int heldTicks = heldTicks(player);
		LivingEntity target = findReleaseTarget(player);
		if (target == null) {
			return;
		}
		ImpactProfile profile = CombatImpactEngine.profileFor(player, data.heroId(), target, heldTicks);
		CombatImpactEngine.applyMeleeImpact(player, target, profile);
	}

	private static int heldTicks(ServerPlayer player) {
		ChargeState state = CHARGES.get(player.getUUID());
		if (state == null) {
			return 0;
		}
		long held = player.level().getGameTime() - state.startedAt;
		return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, held));
	}

	private static boolean isCharging(ServerPlayer player) {
		return CHARGES.containsKey(player.getUUID());
	}

	private static void resetAfterImpact(ServerPlayer player) {
		ChargeState state = CHARGES.get(player.getUUID());
		if (state != null) {
			state.startedAt = player.level().getGameTime();
		}
	}

	private static boolean validTarget(ServerPlayer attacker, LivingEntity target) {
		if (target == attacker || !target.isAlive() || target.isSpectator()) {
			return false;
		}
		return !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
	}

	private static LivingEntity findReleaseTarget(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 direction = player.getViewVector(1f).normalize();
		if (direction.lengthSqr() < 1.0e-4) {
			return null;
		}
		Vec3 end = eye.add(direction.scale(RELEASE_RANGE));
		BlockHitResult blockHit = player.serverLevel().clip(new ClipContext(
				eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 entityEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : end;
		AABB box = player.getBoundingBox().expandTowards(direction.scale(RELEASE_RANGE)).inflate(RELEASE_HIT_INFLATE);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.serverLevel(), player, eye, entityEnd, box,
				entity -> entity instanceof LivingEntity target && validTarget(player, target));
		if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
			return null;
		}
		return target;
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
			}
		}
	}

	private static final class ChargeState {
		private long startedAt;

		private ChargeState(long startedAt) {
			this.startedAt = startedAt;
		}
	}
}
