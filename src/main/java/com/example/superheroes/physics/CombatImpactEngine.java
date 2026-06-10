package com.example.superheroes.physics;

import com.example.superheroes.hero.ATrainHero;
import com.example.superheroes.hero.BattleBeastHero;
import com.example.superheroes.hero.CaptainAmericaHero;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.hero.GokuHero;
import com.example.superheroes.hero.HomelanderHero;
import com.example.superheroes.hero.InvincibleHero;
import com.example.superheroes.hero.IronManHero;
import com.example.superheroes.hero.KazuhaHero;
import com.example.superheroes.hero.KratosHero;
import com.example.superheroes.hero.LokiHero;
import com.example.superheroes.hero.NarutoHero;
import com.example.superheroes.hero.OmnimanHero;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.hero.RegulusHero;
import com.example.superheroes.hero.ReinhardHero;
import com.example.superheroes.hero.RemHero;
import com.example.superheroes.hero.ScaramoucheHero;
import com.example.superheroes.hero.SungJinwooHero;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.network.WallImpactDebrisS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CombatImpactEngine {
	private static final double SWEEP_RADIUS_TIER_2 = 2.6;
	private static final double SWEEP_RADIUS_TIER_3 = 4.4;

	private CombatImpactEngine() {
	}

	public static ImpactProfile profileFor(ServerPlayer attacker, ResourceLocation heroId, LivingEntity target, int heldTicks) {
		ImpactTier tier = ImpactChargeRules.tierFor(heldTicks);
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
		double launchPower;
		float variance;
		double shakeRadius;
		float shakeIntensity;
		float debrisIntensity;

		if (tier == ImpactTier.TIER_3) {
			variance = 0.7f + attacker.getRandom().nextFloat() * 0.7f;
			damage = (float) (baseDamage * 3.0 * heroPower * weaponBias * attackCooldown * variance);
			knockback = 4.5 * heroPower * speedBias * variance;
			upward = Math.min(0.5, 0.32 + 0.13 * variance);
			launchPower = 90.0 * heroPower * variance;
			shakeRadius = 38.0;
			shakeIntensity = (float) Math.min(3.2, 1.75 * heroPower * speedBias * variance);
			debrisIntensity = Math.min(1.5f, 0.85f * variance);
		} else if (tier == ImpactTier.TIER_2) {
			variance = 1.0f;
			damage = (float) (baseDamage * 1.8 * heroPower * weaponBias * attackCooldown);
			knockback = 2.6 * heroPower * speedBias;
			upward = 0.4;
			launchPower = 35.0 * heroPower;
			shakeRadius = 24.0;
			shakeIntensity = (float) Math.min(2.0, 0.95 * heroPower * speedBias);
			debrisIntensity = 0.55f;
		} else {
			variance = 1.0f;
			damage = (float) (baseDamage * 0.95 * heroPower * attackCooldown);
			knockback = 0.9 * heroPower * speedBias;
			upward = 0.12;
			launchPower = 0.0;
			shakeRadius = 12.0;
			shakeIntensity = 0.32f;
			debrisIntensity = 0.0f;
		}

		return new ImpactProfile(heroId, tier, style, direction, damage, knockback, upward,
				launchPower, variance, shakeRadius, shakeIntensity, debrisIntensity);
	}

	public static boolean applyMeleeImpact(ServerPlayer attacker, LivingEntity target, ImpactProfile profile) {
		ServerLevel level = attacker.serverLevel();
		if (!target.hurt(attacker.damageSources().playerAttack(attacker), profile.damage())) {
			return false;
		}

		Vec3 direction = profile.direction();
		applyKnockback(target, direction, profile.knockback(), profile.upwardKnockback());
		if (profile.launchPower() > 0.0) {
			BallisticBodyTracker.launch(target, target.getDeltaMovement(), profile.launchPower(), attacker);
		}
		sweepNearby(level, attacker, target, centerOf(target), profile);
		spawnServerFeedback(level, target, profile);
		shake(level, centerOf(target), profile);
		if (profile.tier() != ImpactTier.TIER_1) {
			sendDebris(level, centerOf(target).add(direction.scale(0.85)), direction, profile);
		}
		attacker.swing(attacker.getUsedItemHand());
		attacker.resetAttackStrengthTicker();
		return true;
	}

	public static void applyKnockback(LivingEntity target, Vec3 direction, double horizontal, double upward) {
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
				if (profile.launchPower() > 0.0) {
					BallisticBodyTracker.launch(target, target.getDeltaMovement(),
							profile.launchPower() * 0.4 * (0.35 + falloff * 0.65), attacker);
				}
			}
		}
	}

	private static void spawnServerFeedback(ServerLevel level, LivingEntity target, ImpactProfile profile) {
		Vec3 c = centerOf(target);
		float variance = profile.variance();
		int crit = profile.tier() == ImpactTier.TIER_3 ? Math.round(44 * variance)
				: profile.tier() == ImpactTier.TIER_2 ? 24 : 8;
		level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, crit, 0.45, 0.35, 0.45, 0.22);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z,
				profile.tier() == ImpactTier.TIER_3 ? 5 : 1, 0.7, 0.18, 0.7, 0.0);
		if (profile.tier() != ImpactTier.TIER_1) {
			level.sendParticles(ParticleTypes.POOF, c.x, c.y, c.z,
					profile.tier() == ImpactTier.TIER_3 ? Math.round(60 * variance) : 34, 1.1, 0.55, 1.1, 0.12);
			level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z,
					profile.tier() == ImpactTier.TIER_3 ? Math.max(2, Math.round(4 * variance)) : 1, 0.45, 0.2, 0.45, 0.0);
		}
		if (profile.tier() == ImpactTier.TIER_3) {
			spawnTierThreeVariant(level, c, profile);
		}
		float pitch = profile.tier() == ImpactTier.TIER_3 ? 0.58f / Math.max(0.8f, variance)
				: profile.tier() == ImpactTier.TIER_2 ? 0.78f : 1.05f;
		level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, pitch);
		if (profile.tier() != ImpactTier.TIER_1) {
			level.playSound(null, c.x, c.y, c.z, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS,
					profile.tier() == ImpactTier.TIER_3 ? 1.8f : 1.15f, pitch * 0.8f);
		}
		if (profile.tier() == ImpactTier.TIER_3) {
			level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
					Math.min(1.8f, 1.1f + 0.4f * variance), 0.5f + 0.15f * variance);
		}
	}

	private static void spawnTierThreeVariant(ServerLevel level, Vec3 c, ImpactProfile profile) {
		float variance = profile.variance();
		int variant = level.random.nextInt(3);
		if (variant == 0) {
			int points = Math.round(26 * variance);
			for (int i = 0; i < points; i++) {
				double angle = (Math.PI * 2.0 * i) / points;
				double radius = 1.6 + variance * 0.8;
				level.sendParticles(ParticleTypes.SWEEP_ATTACK,
						c.x + Math.cos(angle) * radius, c.y - 0.2, c.z + Math.sin(angle) * radius,
						1, 0.05, 0.05, 0.05, 0.0);
			}
			level.sendParticles(ParticleTypes.GUST, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
		} else if (variant == 1) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z,
					Math.round(48 * variance), 0.9, 0.7, 0.9, 0.35);
			level.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.playSound(null, c.x, c.y, c.z, SoundEvents.TRIDENT_THUNDER.value(),
					SoundSource.PLAYERS, 0.7f, 1.2f);
		} else {
			level.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z,
					Math.round(40 * variance), 1.2, 0.6, 1.2, 0.18);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z,
					Math.round(26 * variance), 0.9, 0.5, 0.9, 0.1);
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

	private static void sendDebris(ServerLevel level, Vec3 position, Vec3 direction, ImpactProfile profile) {
		WallImpactDebrisS2CPayload payload = new WallImpactDebrisS2CPayload(
				position, direction, profile.debrisIntensity(), new int[0]);
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

	public static double heroPowerOf(ResourceLocation heroId) {
		return heroPower(heroId);
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
		if (ThanosHero.ID.equals(heroId)) return 1.25;
		if (InvincibleHero.ID.equals(heroId)) return 1.22;
		if (GokuHero.ID.equals(heroId)) return 1.20;
		if (KratosHero.ID.equals(heroId)) return 1.16;
		if (HomelanderHero.ID.equals(heroId)) return 1.15;
		if (SungJinwooHero.ID.equals(heroId)) return 1.12;
		if (RegulusHero.ID.equals(heroId)) return 1.10;
		if (RemHero.ID.equals(heroId)) return 1.08;
		if (RaidenHero.ID.equals(heroId)) return 1.05;
		if (ReinhardHero.ID.equals(heroId)) return 1.05;
		if (NarutoHero.ID.equals(heroId)) return 1.00;
		if (IronManHero.ID.equals(heroId)) return 1.00;
		if (KazuhaHero.ID.equals(heroId)) return 0.95;
		if (LokiHero.ID.equals(heroId)) return 0.95;
		if (ATrainHero.ID.equals(heroId)) return 0.95;
		if (ScaramoucheHero.ID.equals(heroId)) return 0.92;
		if (CaptainAmericaHero.ID.equals(heroId)) return 0.88;
		return 1.0;
	}
}
