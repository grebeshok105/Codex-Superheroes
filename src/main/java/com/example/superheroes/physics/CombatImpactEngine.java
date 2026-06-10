package com.example.superheroes.physics;

import com.example.superheroes.hero.ATrainHero;
import com.example.superheroes.hero.BattleBeastHero;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.hero.HomelanderHero;
import com.example.superheroes.hero.InvincibleHero;
import com.example.superheroes.hero.IronManHero;
import com.example.superheroes.hero.OmnimanHero;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.hero.RemHero;
import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.network.WallImpactDebrisS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class CombatImpactEngine {
	private static final double SWEEP_RADIUS_TIER_2 = 2.6;
	private static final double SWEEP_RADIUS_TIER_3 = 4.4;
	private static final int DEBRIS_STATE_LIMIT = 8;

	private CombatImpactEngine() {
	}

	public static ImpactProfile profileFor(ServerPlayer attacker, ResourceLocation heroId, LivingEntity target, int heldTicks) {
		ImpactTier tier = ImpactChargeRules.tierFor(heldTicks);
		float charge = ImpactChargeRules.chargeScale(heldTicks);
		Vec3 direction = direction(attacker, target);
		ImpactStyle style = styleFor(heroId);
		double heroPower = heroPower(heroId);
		double speedBias = style == ImpactStyle.SPEED ? 1.22 : 1.0;
		double weaponBias = style == ImpactStyle.WEAPON ? 1.12 : 1.0;
		float baseDamage = (float) Math.max(3.0, attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
		float attackCooldown = Math.max(0.25f, attacker.getAttackStrengthScale(0.5f));

		float damage;
		double knockback;
		double upward;
		boolean breakBlocks;
		boolean wallPierce;
		double breakRadius;
		int maxBlocks;
		float maxHardness;
		double shakeRadius;
		float shakeIntensity;
		float debrisIntensity;

		if (tier == ImpactTier.TIER_3) {
			damage = (float) (baseDamage * (2.75 + charge * 0.95) * heroPower * weaponBias * attackCooldown);
			knockback = 4.25 * heroPower * speedBias;
			upward = 0.78 + charge * 0.25;
			breakBlocks = true;
			wallPierce = true;
			breakRadius = 3.15;
			maxBlocks = 84;
			maxHardness = 18.0f;
			shakeRadius = 38.0;
			shakeIntensity = (float) Math.min(3.2, 1.75 * heroPower * speedBias);
			debrisIntensity = 1.15f;
		} else if (tier == ImpactTier.TIER_2) {
			damage = (float) (baseDamage * (1.65 + charge * 0.45) * heroPower * weaponBias * attackCooldown);
			knockback = 2.15 * heroPower * speedBias;
			upward = 0.42 + charge * 0.16;
			breakBlocks = true;
			wallPierce = false;
			breakRadius = 1.75;
			maxBlocks = 22;
			maxHardness = 8.0f;
			shakeRadius = 24.0;
			shakeIntensity = (float) Math.min(2.0, 0.95 * heroPower * speedBias);
			debrisIntensity = 0.65f;
		} else {
			damage = (float) (baseDamage * (0.92 + charge * 0.18) * heroPower * attackCooldown);
			knockback = 0.82 * heroPower * speedBias;
			upward = 0.16;
			breakBlocks = false;
			wallPierce = false;
			breakRadius = 0.0;
			maxBlocks = 0;
			maxHardness = 0f;
			shakeRadius = 12.0;
			shakeIntensity = 0.32f;
			debrisIntensity = 0.0f;
		}

		return new ImpactProfile(heroId, tier, style, direction, damage, knockback, upward,
				breakBlocks, wallPierce, breakRadius, maxBlocks, maxHardness,
				shakeRadius, shakeIntensity, debrisIntensity);
	}

	public static boolean applyMeleeImpact(ServerPlayer attacker, LivingEntity target, ImpactProfile profile) {
		ServerLevel level = attacker.serverLevel();
		if (!target.hurt(attacker.damageSources().playerAttack(attacker), profile.damage())) {
			return false;
		}

		Vec3 direction = profile.direction();
		applyKnockback(target, direction, profile.knockback(), profile.upwardKnockback());
		sweepNearby(level, attacker, target, centerOf(target), profile);
		List<Integer> debrisStates = profile.breakBlocks()
				? breakImpactBlocks(level, attacker, centerOf(target), direction, profile)
				: List.of();
		spawnServerFeedback(level, target, profile);
		shake(level, centerOf(target), profile);
		if (profile.tier() != ImpactTier.TIER_1) {
			sendDebris(level, centerOf(target).add(direction.scale(0.85)), direction, profile, debrisStates);
		}
		attacker.swing(attacker.getUsedItemHand());
		attacker.resetAttackStrengthTicker();
		return true;
	}

	private static void applyKnockback(LivingEntity target, Vec3 direction, double horizontal, double upward) {
		Vec3 motion = direction.scale(horizontal);
		target.setDeltaMovement(motion.x, upward, motion.z);
		target.hurtMarked = true;
		target.hasImpulse = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	private static void sweepNearby(ServerLevel level, ServerPlayer attacker, LivingEntity primary, Vec3 impact, ImpactProfile profile) {
		double radius = profile.tier() == ImpactTier.TIER_3 ? SWEEP_RADIUS_TIER_3
				: profile.tier() == ImpactTier.TIER_2 ? SWEEP_RADIUS_TIER_2 : 0.0;
		if (radius <= 0.0) {
			return;
		}
		AABB box = new AABB(impact, impact).inflate(radius);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
				entity -> entity != primary && validSweepTarget(attacker, entity))) {
			Vec3 center = centerOf(target);
			double distanceSqr = center.distanceToSqr(impact);
			if (distanceSqr > radius * radius) {
				continue;
			}
			double falloff = 1.0 - Math.sqrt(distanceSqr) / radius;
			Vec3 away = center.subtract(impact);
			if (away.lengthSqr() < 1.0e-4) {
				away = profile.direction();
			} else {
				away = away.normalize();
			}
			float damage = (float) (profile.damage() * 0.34 * (0.35 + falloff * 0.65));
			if (target.hurt(attacker.damageSources().playerAttack(attacker), damage)) {
				applyKnockback(target, away.add(profile.direction().scale(0.35)).normalize(),
						profile.knockback() * 0.52 * (0.35 + falloff * 0.65),
						profile.upwardKnockback() * 0.75);
			}
		}
	}

	private static List<Integer> breakImpactBlocks(ServerLevel level, ServerPlayer attacker, Vec3 impact, Vec3 direction, ImpactProfile profile) {
		ArrayList<Integer> states = new ArrayList<>(DEBRIS_STATE_LIMIT);
		int destroyed = 0;
		int passes = profile.wallPierce() ? 4 : 1;
		for (int i = 0; i < passes && destroyed < profile.maxBlocks(); i++) {
			Vec3 center = impact.add(direction.scale(0.8 + i * 1.15));
			destroyed += breakSphere(level, attacker, center, profile.breakRadius(), profile,
					profile.maxBlocks() - destroyed, states);
		}
		return states;
	}

	private static int breakSphere(ServerLevel level, ServerPlayer attacker, Vec3 center, double radius,
			ImpactProfile profile, int limit, List<Integer> debrisStates) {
		if (limit <= 0) {
			return 0;
		}
		int destroyed = 0;
		int blockRadius = (int) Math.ceil(radius);
		double radiusSqr = radius * radius;
		BlockPos centerPos = BlockPos.containing(center);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int shell = 0; shell <= blockRadius && destroyed < limit; shell++) {
			for (int dx = -shell; dx <= shell && destroyed < limit; dx++) {
				for (int dy = -shell; dy <= shell && destroyed < limit; dy++) {
					for (int dz = -shell; dz <= shell && destroyed < limit; dz++) {
						if (Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)) != shell) {
							continue;
						}
						pos.set(centerPos.getX() + dx, centerPos.getY() + dy, centerPos.getZ() + dz);
						Vec3 blockCenter = Vec3.atCenterOf(pos);
						if (blockCenter.distanceToSqr(center) > radiusSqr) {
							continue;
						}
						BlockState state = level.getBlockState(pos);
						if (!BlockBreakPolicy.canImpactBreak(level, pos, state, profile.maxHardness())) {
							continue;
						}
						if (debrisStates.size() < DEBRIS_STATE_LIMIT) {
							debrisStates.add(Block.getId(state));
						}
						if (level.destroyBlock(pos.immutable(), false, attacker)) {
							destroyed++;
						}
					}
				}
			}
		}
		return destroyed;
	}

	private static void spawnServerFeedback(ServerLevel level, LivingEntity target, ImpactProfile profile) {
		Vec3 c = centerOf(target);
		int crit = profile.tier() == ImpactTier.TIER_3 ? 44 : profile.tier() == ImpactTier.TIER_2 ? 24 : 8;
		level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, crit, 0.45, 0.35, 0.45, 0.22);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z,
				profile.tier() == ImpactTier.TIER_3 ? 5 : 1, 0.7, 0.18, 0.7, 0.0);
		if (profile.tier() != ImpactTier.TIER_1) {
			level.sendParticles(ParticleTypes.POOF, c.x, c.y, c.z,
					profile.tier() == ImpactTier.TIER_3 ? 72 : 34, 1.1, 0.55, 1.1, 0.12);
			level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z,
					profile.tier() == ImpactTier.TIER_3 ? 4 : 1, 0.45, 0.2, 0.45, 0.0);
		}
		float pitch = profile.tier() == ImpactTier.TIER_3 ? 0.58f : profile.tier() == ImpactTier.TIER_2 ? 0.78f : 1.05f;
		level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, pitch);
		if (profile.tier() != ImpactTier.TIER_1) {
			level.playSound(null, c.x, c.y, c.z, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS,
					profile.tier() == ImpactTier.TIER_3 ? 1.8f : 1.15f, pitch * 0.8f);
		}
		if (profile.tier() == ImpactTier.TIER_3) {
			level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.35f, 0.55f);
		}
	}

	private static void shake(ServerLevel level, Vec3 center, ImpactProfile profile) {
		if (profile.shakeIntensity() <= 0.01f) {
			return;
		}
		for (ServerPlayer nearby : PlayerLookup.around(level, center, profile.shakeRadius())) {
			double distance = nearby.position().distanceTo(center);
			float falloff = (float) Math.max(0.0, 1.0 - distance / profile.shakeRadius());
			float intensity = profile.shakeIntensity() * (0.25f + falloff * 0.75f);
			int duration = profile.tier() == ImpactTier.TIER_3 ? 28 : profile.tier() == ImpactTier.TIER_2 ? 18 : 8;
			ServerPlayNetworking.send(nearby, new ScreenShakeS2CPayload(intensity, duration));
		}
	}

	private static void sendDebris(ServerLevel level, Vec3 position, Vec3 direction, ImpactProfile profile, List<Integer> debrisStates) {
		int[] ids = debrisStates.stream().mapToInt(Integer::intValue).toArray();
		WallImpactDebrisS2CPayload payload = new WallImpactDebrisS2CPayload(
				position, direction, profile.debrisIntensity(), ids);
		for (ServerPlayer nearby : PlayerLookup.around(level, position, 48.0)) {
			ServerPlayNetworking.send(nearby, payload);
		}
	}

	private static boolean validSweepTarget(ServerPlayer attacker, LivingEntity entity) {
		if (entity == attacker || !entity.isAlive() || entity.isSpectator()) {
			return false;
		}
		return !(entity instanceof Player player && player.isCreative());
	}

	private static Vec3 centerOf(LivingEntity entity) {
		return entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
	}

	private static Vec3 direction(ServerPlayer attacker, LivingEntity target) {
		Vec3 fromAttacker = centerOf(target).subtract(attacker.position().add(0.0, attacker.getBbHeight() * 0.55, 0.0));
		if (fromAttacker.lengthSqr() > 1.0e-4) {
			return new Vec3(fromAttacker.x, Math.max(-0.25, fromAttacker.y), fromAttacker.z).normalize();
		}
		Vec3 view = attacker.getViewVector(1f);
		return view.lengthSqr() > 1.0e-4 ? view.normalize() : new Vec3(0.0, 0.0, 1.0);
	}

	public static ImpactStyle styleOf(ResourceLocation heroId) {
		return styleFor(heroId);
	}

	private static ImpactStyle styleFor(ResourceLocation heroId) {
		if (ATrainHero.ID.equals(heroId)) return ImpactStyle.SPEED;
		if (IronManHero.ID.equals(heroId)) return ImpactStyle.ENERGY;
		if (RaidenHero.ID.equals(heroId) || RemHero.ID.equals(heroId)) return ImpactStyle.WEAPON;
		if (DoomsdayHero.ID.equals(heroId)
				|| BattleBeastHero.ID.equals(heroId)
				|| OmnimanHero.ID.equals(heroId)
				|| InvincibleHero.ID.equals(heroId)
				|| HomelanderHero.ID.equals(heroId)) {
			return ImpactStyle.BRUTAL;
		}
		return ImpactStyle.DEFAULT;
	}

	private static double heroPower(ResourceLocation heroId) {
		if (DoomsdayHero.ID.equals(heroId)) return 1.34;
		if (BattleBeastHero.ID.equals(heroId)) return 1.28;
		if (OmnimanHero.ID.equals(heroId)) return 1.27;
		if (InvincibleHero.ID.equals(heroId)) return 1.22;
		if (HomelanderHero.ID.equals(heroId)) return 1.15;
		if (RemHero.ID.equals(heroId)) return 1.08;
		if (ATrainHero.ID.equals(heroId)) return 0.95;
		return 1.0;
	}
}
