package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.resource.ResourceController;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Репульсор Железного Человека. Обычный выстрел — точный луч 40 блоков, 8 урона.
 * Заряженный выстрел (в присяде, sneak) — мощный широкий импульс: больше урона,
 * толстый двойной луч, ударная волна по площади у точки попадания и сильный
 * отброс. Заряженный режим дополнительно тратит энергию.
 */
public final class RepulsorAbility implements Ability {
	private static final double RANGE = 40.0;
	private static final float DAMAGE = 8.0f;
	private static final float CHARGED_DAMAGE = 22.0f;
	private static final float CHARGED_EXTRA_COST = 300f;
	private static final double CHARGED_AOE = 3.5;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REPULSOR;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 200f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		// Динамический заряд 0..1 от длительности присяда (RepulsorChargeController).
		float chargeAmt = player.isShiftKeyDown()
				? net.minecraft.util.Mth.clamp(RepulsorChargeController.charge(player), 0f, 1f)
				: 0f;
		boolean charged = chargeAmt > 0.12f
				&& ResourceController.tryConsume(player, getId(), CHARGED_EXTRA_COST * chargeAmt);
		if (!charged) {
			chargeAmt = 0f;
		}

		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);
		Vec3 end = eye.add(dir.scale(RANGE));
		BlockHitResult bh = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 actualEnd = bh.getType() == HitResult.Type.BLOCK ? bh.getLocation() : end;
		// Урон плавно растёт с зарядом: 12 (порог) → 22 (полный).
		float dmg = charged ? net.minecraft.util.Mth.lerp(chargeAmt, DAMAGE * 1.5f, CHARGED_DAMAGE) : DAMAGE;
		double aoe = CHARGED_AOE * (0.5 + 0.5 * chargeAmt);

		AABB box = player.getBoundingBox().expandTowards(dir.scale(RANGE)).inflate(charged ? 0.7 + 0.6 * chargeAmt : 0.6);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, actualEnd, box,
				e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator());
		if (hit != null) {
			LivingEntity target = (LivingEntity) hit.getEntity();
			target.hurt(ModDamageTypes.repulsor(level, player), dmg);
			actualEnd = hit.getLocation();
			double kb = charged ? 0.8 + 1.0 * chargeAmt : 0.6;
			Vec3 push = dir.scale(kb);
			target.push(push.x, charged ? 0.3 + 0.3 * chargeAmt : 0.2, push.z);
			target.hurtMarked = true;
		}

		if (charged) {
			// ударная волна у точки попадания (радиус растёт с зарядом)
			List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
					new AABB(actualEnd, actualEnd).inflate(aoe),
					e -> e.isAlive() && e != player && !e.isSpectator());
			for (LivingEntity e : nearby) {
				double dist = e.position().distanceTo(actualEnd);
				float falloff = (float) Math.max(0.25, 1.0 - dist / (aoe + 1.0));
				e.hurt(ModDamageTypes.repulsor(level, player), CHARGED_DAMAGE * 0.6f * falloff * (0.5f + 0.5f * chargeAmt));
				Vec3 away = e.position().subtract(actualEnd).normalize().scale(0.9 * falloff * (0.5 + 0.5 * chargeAmt));
				e.push(away.x, 0.4 * falloff, away.z);
				e.hurtMarked = true;
			}
		}

		Vec3 hand = eye.add(dir.scale(0.5));
		ModNetworking.broadcastRepulsor(player, hand, actualEnd);
		if (charged) {
			// толстый луч + до двух доп. боковых лучей при сильном заряде
			Vec3 side = dir.cross(new Vec3(0, 1, 0)).normalize().scale(0.10 + 0.10 * chargeAmt);
			ModNetworking.broadcastRepulsor(player, hand.add(side), actualEnd.add(side));
			ModNetworking.broadcastRepulsor(player, hand.subtract(side), actualEnd.subtract(side));
			if (chargeAmt > 0.7f) {
				Vec3 up = new Vec3(0, 0.12, 0);
				ModNetworking.broadcastRepulsor(player, hand.add(up), actualEnd.add(up));
				ModNetworking.broadcastRepulsor(player, hand.subtract(up), actualEnd.subtract(up));
			}
		}

		int sparks = charged ? 40 : 18;
		level.sendParticles(ModParticles.REPULSOR_SPARK,
				hand.x, hand.y, hand.z, sparks, 0.25, 0.25, 0.25, charged ? 0.12 : 0.05);
		level.sendParticles(ParticleTypes.SMOKE,
				hand.x, hand.y, hand.z, charged ? 16 : 8, 0.2, 0.2, 0.2, 0.02);
		level.sendParticles(ModParticles.REPULSOR_SPARK,
				actualEnd.x, actualEnd.y, actualEnd.z, charged ? 34 : 14, 0.3, 0.3, 0.3, 0.08);
		level.sendParticles(ParticleTypes.END_ROD,
				actualEnd.x, actualEnd.y, actualEnd.z, charged ? 12 : 4, 0.2, 0.2, 0.2, 0.05);
		if (charged) {
			level.sendParticles(ParticleTypes.EXPLOSION,
					actualEnd.x, actualEnd.y, actualEnd.z, 2, 0.2, 0.2, 0.2, 0.0);
			level.sendParticles(ParticleTypes.FLASH,
					actualEnd.x, actualEnd.y, actualEnd.z, 1, 0, 0, 0, 0.0);
		}

		float pitch = charged ? 0.8f : 1.6f;
		float vol = charged ? 1.0f : 0.7f;
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, vol, pitch);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, vol, pitch);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, charged ? 0.9f : 0.5f, charged ? 1.0f : 1.4f);
		if (charged) {
			level.playSound(null, actualEnd.x, actualEnd.y, actualEnd.z,
					SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
			// разрядили накопленный заряд
			RepulsorChargeController.reset(player);
		}
		return true;
	}
}
