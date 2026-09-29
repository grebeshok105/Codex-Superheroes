package io.github.grebeshok105.codex.hero.homelander.ability;

import io.github.grebeshok105.codex.hero.homelander.effect.HomelanderEffects;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.hero.homelander.registry.HomelanderDamageTypes;
import io.github.grebeshok105.codex.hero.homelander.runtime.UraniumDefenseController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.net.VfxChannelS2CPayload;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.particle.SilentParticles;
import io.github.grebeshok105.codex.mechanic.world.WorldDestructionPolicy;
import io.github.grebeshok105.codex.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class EyeLasersAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("eye_lasers");
	private static final double RANGE = 64.0;
	private static final float MIN_DPS = 56.0f;
	private static final float MAX_DPS = 120.0f;
	private static final float MADNESS_DAMAGE_MUL = 3.0f;
	private static final double CHEST_FRACTION = 0.7;

	/**
	 * Пульсирующий паттерн (как у уставшего стрелка): 
	 * 0..19 (20t = 1c) shoot → 20..29 (10t = 0.5c) pause →
	 * 30..69 (40t = 2c) shoot → 70..79 (10t = 0.5c) pause → cycle.
	 * Полный цикл = 80 тиков (4 секунды). Длинный сегмент даёт ощущение «всё-таки
	 * стреляет», паузы — что не может стрелять непрерывно.
	 */
	private static final int PULSE_CYCLE_TICKS = 80;
	private static final int PULSE_PHASE_SHOT1_END = 20;
	private static final int PULSE_PHASE_PAUSE1_END = 30;
	private static final int PULSE_PHASE_SHOT2_END = 70;

	// No lifecycle clearOn: entries are dropped by onDeactivate / the non-threat
	// branch of onTickActive, exactly like the old map.
	private static final OwnedSessionMap<UUID, Integer> PULSE_TICK =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));

	// Ticks since activation — the VFX channel heartbeat clock. Separate from
	// PULSE_TICK: it counts through the uranium pauses PULSE_TICK pauses on.
	private static final OwnedSessionMap<UUID, Integer> ACTIVE_TICK =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 4.0f;
	}

	@Override
	public float costPerTick() {
		return 1.5f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		PULSE_TICK.put(player.getUUID(), player.getUUID(), 0);
		ACTIVE_TICK.put(player.getUUID(), player.getUUID(), 0);
		Vec3 end = fireBeam(player);
		VfxFx.channel(player, HomelanderVfxIds.LASER, VfxChannelS2CPayload.START, end);
		return true;
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		PULSE_TICK.remove(player.getUUID());
		ACTIVE_TICK.remove(player.getUUID());
		VfxFx.channel(player, HomelanderVfxIds.LASER, VfxChannelS2CPayload.STOP,
				player.getEyePosition());
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		Integer storedActive = ACTIVE_TICK.get(player.getUUID());
		int activeTicks = storedActive == null ? 0 : storedActive;
		if (EyeLaserPhases.shouldSendUpdate(activeTicks)) {
			VfxFx.channel(player, HomelanderVfxIds.LASER, VfxChannelS2CPayload.UPDATE,
					raycastBeam(player).actualEnd());
		}
		ACTIVE_TICK.put(player.getUUID(), player.getUUID(), activeTicks + 1);

		boolean madness = HomelanderEffects.isMadness(player);
		boolean uraniumThreat = UraniumDefenseController.isUnderUraniumThreat(player);
		boolean fire;
		if (madness || !uraniumThreat) {
			PULSE_TICK.remove(player.getUUID());
			fire = true;
		} else {
			Integer stored = PULSE_TICK.get(player.getUUID());
			int phase = stored == null ? 0 : stored;
			if (phase < PULSE_PHASE_SHOT1_END) {
				fire = true;
			} else if (phase < PULSE_PHASE_PAUSE1_END) {
				fire = false;
			} else if (phase < PULSE_PHASE_SHOT2_END) {
				fire = true;
			} else {
				fire = false;
			}
			phase++;
			if (phase >= PULSE_CYCLE_TICKS) phase = 0;
			PULSE_TICK.put(player.getUUID(), player.getUUID(), phase);
		}
		if (fire) {
			fireBeam(player);
		}
	}

	private record BeamRaycast(@Nullable EntityHitResult hit, BlockHitResult blockHit,
			Vec3 actualEnd) {
	}

	private static BeamRaycast raycastBeam(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1f);
		Vec3 end = eye.add(dir.scale(RANGE));
		ServerLevel level = player.serverLevel();
		BlockHitResult blockHit = level.clip(new ClipContext(
				eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 entitySearchEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : end;
		AABB box = player.getBoundingBox().expandTowards(dir.scale(RANGE)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(
				level, player, eye, entitySearchEnd, box,
				e -> e instanceof LivingEntity le && TargetFilters.hostileTo(player).test(le));
		Vec3 actualEnd = entitySearchEnd;
		if (hit != null && hit.getEntity() instanceof LivingEntity target) {
			actualEnd = new Vec3(target.getX(), target.getY() + target.getBbHeight() * CHEST_FRACTION,
					target.getZ());
		}
		return new BeamRaycast(hit, blockHit, actualEnd);
	}

	private static Vec3 fireBeam(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		boolean madness = HomelanderEffects.isMadness(player);
		BeamRaycast ray = raycastBeam(player);
		EntityHitResult hit = ray.hit();
		Vec3 actualEnd = ray.actualEnd();
		float damage = damagePerTick(player) * (madness ? MADNESS_DAMAGE_MUL : 1f);
		boolean choppy = false;
		if (hit != null && hit.getEntity() instanceof net.minecraft.world.entity.player.Player victim
				&& UraniumDefenseController.hasUraniumDagger(victim)) {
			damage *= 0.5f;
			int phase = player.tickCount % 15;
			choppy = phase < 5;
			if (choppy) damage = 0f;
		}
		if (hit != null) {
			LivingEntity target = (LivingEntity) hit.getEntity();
			if (damage > 0f) target.hurt(HomelanderDamageTypes.eyeLaser(level, player), damage);
			if (madness) {
				if (player.tickCount % 2 == 0) {
					level.explode(player, null, null, actualEnd.x, actualEnd.y, actualEnd.z,
							2.4f, true, Level.ExplosionInteraction.MOB,
							SilentParticles.SILENT, SilentParticles.SILENT,
							BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SILENT));
					target.igniteForSeconds(8f);
				}
				placeFireRing(level, player, actualEnd, 3);
			}
		} else if (madness && ray.blockHit().getType() == HitResult.Type.BLOCK) {
			if (player.tickCount % 2 == 0) {
				level.explode(player, null, null, actualEnd.x, actualEnd.y, actualEnd.z,
						2.0f, true, Level.ExplosionInteraction.MOB,
						SilentParticles.SILENT, SilentParticles.SILENT,
						BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SILENT));
			}
			placeFireRing(level, player, actualEnd, 3);
		}
		return actualEnd;
	}

	private static void placeFireRing(ServerLevel level, ServerPlayer player, Vec3 center, int radius) {
		BlockPos centerPos = BlockPos.containing(center);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) {
					continue;
				}
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos pos = centerPos.offset(dx, dy, dz);
					if (!level.getBlockState(pos).isAir()) {
						continue;
					}
					BlockPos below = pos.below();
					if (BaseFireBlock.canBePlacedAt(level, pos, net.minecraft.core.Direction.UP)
							&& !level.getBlockState(below).isAir()) {
						WorldDestructionPolicy.tryPlace(level, pos, Blocks.FIRE.defaultBlockState(), player);
					}
				}
			}
		}
	}

	private static float damagePerTick(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		float frac = 0f;
		if (data.hasHero()) {
			Hero hero = Heroes.get(data.heroId());
			if (hero != null && hero.getManaMax() > 0f) {
				frac = Math.max(0f, Math.min(1f, data.mana() / hero.getManaMax()));
			}
		}
		float dps = MIN_DPS + (MAX_DPS - MIN_DPS) * frac;
		return dps / 20f;
	}
}
