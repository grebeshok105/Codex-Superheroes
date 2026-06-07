package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ReinhardController;
import com.example.superheroes.effect.ReinhardState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Удар Второго Пришествия — разрыв реальности клинком Рейнхарда.
 * Разрубает землю на 50 блоков длины, 6 ширины и 12 глубины,
 * нанося колоссальный урон всему на пути. Доступно только во Втором пришествии.
 */
public final class ReinhardSecondComingRiftAbility implements Ability {
	private static final float COST = 500f;
	private static final int COOLDOWN_TICKS = 30 * 20;
	private static final int SLASH_LENGTH = 50;
	private static final int SLASH_WIDTH = 6;
	private static final int SLASH_DEPTH = 12;
	private static final float SLASH_DAMAGE = 1000f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_SECOND_COMING_RIFT;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return COST;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		if (!state.inSecondComing()) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.reinhard_second_coming_rift.locked"), true);
			return false;
		}
		if (AbilityCooldowns.isOnCooldown(player, getId())) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.reinhard_second_coming_rift.cooldown"), true);
			return false;
		}
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		ServerLevel level = player.serverLevel();

		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
		if (flat.lengthSqr() < 0.001) {
			flat = new Vec3(1, 0, 0);
		}
		final Vec3 lookFlat = flat;
		Vec3 perpendicular = new Vec3(-lookFlat.z, 0, lookFlat.x);
		Vec3 origin = player.position();
		int halfWidth = SLASH_WIDTH / 2;

		for (int i = 1; i <= SLASH_LENGTH; i++) {
			Vec3 point = origin.add(lookFlat.scale(i));

			for (int w = -halfWidth; w <= halfWidth; w++) {
				Vec3 offset = perpendicular.scale(w);
				int ox = (int) Math.floor(point.x + offset.x);
				int oz = (int) Math.floor(point.z + offset.z);
				int sy = findSurface(level, ox, (int) origin.y, oz);

				for (int dy = 0; dy < SLASH_DEPTH; dy++) {
					BlockPos pos = new BlockPos(ox, sy - dy, oz);
					BlockState state = level.getBlockState(pos);
					if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0
							&& !state.is(Blocks.BEDROCK)
							&& !state.is(Blocks.END_PORTAL_FRAME)
							&& !state.is(Blocks.END_PORTAL)
							&& !state.is(Blocks.BARRIER)) {
						level.destroyBlock(pos, false);
					}
				}
			}

			if (i % 4 == 0) {
				int sy = findSurface(level, (int) Math.floor(point.x), (int) origin.y, (int) Math.floor(point.z));
				level.sendParticles(ParticleTypes.FLASH,
						point.x, sy + 1.0, point.z, 1, 0, 0, 0, 0);
				level.sendParticles(ParticleTypes.END_ROD,
						point.x, sy + 1.5, point.z,
						16, 1.0, 1.2, 1.0, 0.15);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
						point.x, sy + 1.0, point.z,
						18, 0.8, 1.0, 0.8, 0.10);
				level.sendParticles(ParticleTypes.EXPLOSION,
						point.x, sy + 0.8, point.z, 1, 0, 0, 0, 0);
			}
		}

		Vec3 endPoint = origin.add(lookFlat.scale(SLASH_LENGTH));
		AABB slashBox = new AABB(
				Math.min(origin.x, endPoint.x) - SLASH_WIDTH, origin.y - SLASH_DEPTH - 2, Math.min(origin.z, endPoint.z) - SLASH_WIDTH,
				Math.max(origin.x, endPoint.x) + SLASH_WIDTH, origin.y + 6, Math.max(origin.z, endPoint.z) + SLASH_WIDTH
		);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, slashBox,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !(e instanceof Player p && p.getUUID().equals(player.getUUID()))
						&& isInSlashPath(e.position(), origin, lookFlat, perpendicular));
		for (LivingEntity le : targets) {
			le.invulnerableTime = 0;
			le.hurt(level.damageSources().playerAttack(player), SLASH_DAMAGE);
			Vec3 push = le.position().subtract(origin).normalize();
			le.setDeltaMovement(push.x * 1.2, 0.7, push.z * 1.2);
			le.hurtMarked = true;
		}

		for (int n = 0; n < 4; n++) {
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
			if (bolt != null) {
				double dist = (n + 1) * (SLASH_LENGTH / 5.0);
				Vec3 spot = origin.add(lookFlat.scale(dist));
				bolt.moveTo(spot.x, spot.y, spot.z);
				bolt.setVisualOnly(true);
				level.addFreshEntity(bolt);
			}
		}

		level.sendParticles(ParticleTypes.FLASH,
				origin.x + lookFlat.x * 2, origin.y + 1.5, origin.z + lookFlat.z * 2,
				4, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
				origin.x + lookFlat.x * 5, origin.y + 1.5, origin.z + lookFlat.z * 5,
				1, 0, 0, 0, 0);

		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.4f);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 0.6f);
		level.playSound(null, origin.x + lookFlat.x * SLASH_LENGTH * 0.5,
				origin.y, origin.z + lookFlat.z * SLASH_LENGTH * 0.5,
				SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0f, 0.5f);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.2f, 1.6f);
		return true;
	}

	@SuppressWarnings("unused")
	private static boolean ensureReinhardCheck(ServerPlayer player) {
		return ReinhardController.isReinhard(player);
	}

	private static int findSurface(ServerLevel level, int x, int originY, int z) {
		for (int y = originY + 6; y >= originY - 16; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
				return y;
			}
		}
		return originY;
	}

	private static boolean isInSlashPath(Vec3 entityPos, Vec3 origin, Vec3 dir, Vec3 perp) {
		Vec3 diff = entityPos.subtract(origin);
		double along = diff.x * dir.x + diff.z * dir.z;
		if (along < -2 || along > SLASH_LENGTH + 2) return false;
		double across = Math.abs(diff.x * perp.x + diff.z * perp.z);
		return across < (SLASH_WIDTH / 2.0) + 1.5;
	}
}
