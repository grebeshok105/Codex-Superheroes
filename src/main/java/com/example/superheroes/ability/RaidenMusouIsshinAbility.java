package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.RaidenState;
import com.example.superheroes.item.MusouNoHitotachiItem;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Musou Isshin — легендарный удар, разрубивший остров Яширори.
 * Создаёт разрез длиной 35 блоков в направлении взгляда игрока.
 * Разрез — траншея шириной 3 и глубиной 4 блока.
 * Все существа на пути получают огромный урон.
 * КД 45 секунд, стоимость 800 энергии.
 */
public final class RaidenMusouIsshinAbility implements Ability {
	private static final float COST = 800f;
	private static final int COOLDOWN_TICKS = 45 * 20;
	private static final int SLASH_LENGTH = 35;
	private static final int SLASH_WIDTH = 3;
	private static final int SLASH_DEPTH = 4;
	private static final float SLASH_DAMAGE_PLAYER = 30f;
	private static final float SLASH_DAMAGE_MOB = 18f;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.RAIDEN_MUSOU_ISSHIN;
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
		if (AbilityCooldowns.isOnCooldown(player, getId())) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.raiden_musou_isshin.cooldown"), true);
			return false;
		}
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		boolean hasYamato = main.getItem() instanceof MusouNoHitotachiItem || off.getItem() instanceof MusouNoHitotachiItem;
		if (!hasYamato) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.raiden_musou_isshin.no_sword"), true);
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

		for (int i = 2; i <= SLASH_LENGTH; i++) {
			Vec3 point = origin.add(lookFlat.scale(i));
			int bx = (int) Math.floor(point.x);
			int bz = (int) Math.floor(point.z);
			int surfaceY = findSurface(level, bx, (int) origin.y, bz);

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
							&& !state.is(Blocks.OBSIDIAN)
							&& !state.is(Blocks.END_PORTAL_FRAME)
							&& !state.is(Blocks.END_PORTAL)
							&& !state.is(Blocks.BARRIER)) {
						level.destroyBlock(pos, false);
					}
				}
			}

			if (i % 3 == 0) {
				level.sendParticles(ModParticles.ANOMALY_SLICE,
						point.x, surfaceY + 1.0, point.z,
						6, 0.5, 0.8, 0.5, 0.05);
				level.sendParticles(ModParticles.MOONVEIL,
						point.x, surfaceY + 1.5, point.z,
						4, 0.4, 0.6, 0.4, 0.08);
				level.sendParticles(ModParticles.SPARKS,
						point.x, surfaceY + 0.5, point.z,
						12, 0.8, 0.4, 0.8, 0.15);
			}
		}

		AABB slashBox = new AABB(
				origin.x - 2, origin.y - SLASH_DEPTH, origin.z - 2,
				origin.x + lookFlat.x * SLASH_LENGTH + 2,
				origin.y + 3,
				origin.z + lookFlat.z * SLASH_LENGTH + 2
		);
		slashBox = normalizeAABB(slashBox);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, slashBox,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !(e instanceof Player p && p.getUUID().equals(player.getUUID()))
						&& isInSlashPath(e.position(), origin, lookFlat, perpendicular));
		for (LivingEntity le : targets) {
			float dmg = (le instanceof Player) ? SLASH_DAMAGE_PLAYER : SLASH_DAMAGE_MOB;
			le.invulnerableTime = 0;
			le.hurt(level.damageSources().playerAttack(player), dmg);
			Vec3 push = le.position().subtract(origin).normalize();
			le.setDeltaMovement(push.x * 0.8, 0.5, push.z * 0.8);
			le.hurtMarked = true;
		}

		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt != null) {
			Vec3 midPoint = origin.add(lookFlat.scale(SLASH_LENGTH * 0.5));
			bolt.moveTo(midPoint.x, midPoint.y, midPoint.z);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}

		level.sendParticles(ModParticles.WHITE_BOOM,
				origin.x + lookFlat.x * 3, origin.y + 1.5, origin.z + lookFlat.z * 3,
				1, 0, 0, 0, 0);
		level.sendParticles(ModParticles.SWORD_EXPLOSION,
				origin.x + lookFlat.x * 2, origin.y + 1.5, origin.z + lookFlat.z * 2,
				36, 1.0, 0.6, 1.0, 0.2);
		level.sendParticles(ModParticles.SHAMAK,
				origin.x + lookFlat.x * 5, origin.y + 1.0, origin.z + lookFlat.z * 5,
				20, 1.5, 0.5, 1.5, 0.1);

		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5f, 0.5f);
		level.playSound(null, origin.x, origin.y, origin.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.8f);
		level.playSound(null, origin.x + lookFlat.x * SLASH_LENGTH * 0.5,
				origin.y, origin.z + lookFlat.z * SLASH_LENGTH * 0.5,
				SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.4f, 0.6f);

		return true;
	}

	private static int findSurface(ServerLevel level, int x, int originY, int z) {
		for (int y = originY + 6; y >= originY - 10; y--) {
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
		return across < SLASH_WIDTH + 1;
	}

	private static AABB normalizeAABB(AABB box) {
		return new AABB(
				Math.min(box.minX, box.maxX), Math.min(box.minY, box.maxY), Math.min(box.minZ, box.maxZ),
				Math.max(box.minX, box.maxX), Math.max(box.minY, box.maxY), Math.max(box.minZ, box.maxZ)
		);
	}
}
