package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRegistry;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MadnessAftermathController {
	public static final int AFTERMATH_TICKS = 200;
	private static final Set<UUID> hadMadnessLastTick = new HashSet<>();

	private MadnessAftermathController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Set<UUID> stillMadness = new HashSet<>();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				boolean madness = ModEffects.isMadness(player);
				if (madness) {
					stillMadness.add(player.getUUID());
				} else if (hadMadnessLastTick.contains(player.getUUID())) {
					triggerAftermath(player);
				}
				if (ModEffects.isAftermath(player)) {
					tickAftermath(player);
				}
			}
			hadMadnessLastTick.clear();
			hadMadnessLastTick.addAll(stillMadness);
		});
	}

	private static void triggerAftermath(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(ModEffects.MADNESS_AFTERMATH, AFTERMATH_TICKS, 0, false, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, AFTERMATH_TICKS, 4, false, false, true));
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (data.hasHero()) {
			HeroData updated = data;
			for (ResourceLocation abilityId : new HashSet<>(data.activeAbilities())) {
				var ability = AbilityRegistry.get(abilityId);
				if (ability != null) {
					ability.onDeactivate(player);
				}
				updated = updated.withActive(abilityId, false);
			}
			player.setAttached(ModAttachments.HERO_DATA, updated);
			ModNetworking.syncHeroData(player, updated);
		}
		player.serverLevel().playSound(null,
				player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.7f);
	}

	private static void tickAftermath(ServerPlayer player) {
		MobEffectInstance effect = player.getEffect(ModEffects.MADNESS_AFTERMATH);
		if (effect == null) {
			return;
		}
		int remaining = effect.getDuration();
		ServerLevel level = player.serverLevel();
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.resetFallDistance();
		double cx = player.getX();
		double cy = player.getY() + 1.0;
		double cz = player.getZ();
		float t = 1f - (float) remaining / AFTERMATH_TICKS;
		int particles = Mth.floor(2 + 18 * t);
		level.sendParticles(ParticleTypes.END_ROD,
				cx, cy, cz,
				particles, 1.5 + t * 2.0, 1.5, 1.5 + t * 2.0, 0.06);
		level.sendParticles(ParticleTypes.SMALL_FLAME,
				cx, cy, cz,
				particles, 1.0 + t * 1.5, 1.0, 1.0 + t * 1.5, 0.05);
		if (player.tickCount % Math.max(2, 30 - (int)(t * 28)) == 0) {
			level.playSound(null, cx, cy, cz,
					SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.5f + t * 0.5f, 0.8f + t * 1.4f);
		}
		if (player.tickCount % 25 == 0 && t > 0.3f) {
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
			if (bolt != null) {
				double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
				double r = 4.0 + level.getRandom().nextDouble() * 6.0;
				bolt.moveTo(cx + Math.cos(angle) * r, cy - 1.0, cz + Math.sin(angle) * r);
				bolt.setVisualOnly(true);
				level.addFreshEntity(bolt);
			}
		}
		if (remaining <= 1) {
			detonateSun(player);
		}
	}

	private static void detonateSun(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		level.explode(player, x, y, z, 12f, true, Level.ExplosionInteraction.MOB);
		level.explode(player, x, y, z, 7f, true, Level.ExplosionInteraction.MOB);
		BlockPos centerPos = BlockPos.containing(x, y, z);
		int r = 12;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int dist2 = dx * dx + dz * dz;
				if (dist2 > r * r) {
					continue;
				}
				for (int dy = -2; dy <= 4; dy++) {
					BlockPos pos = centerPos.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					if (!state.isAir()) {
						continue;
					}
					BlockPos below = pos.below();
					if (BaseFireBlock.canBePlacedAt(level, pos, net.minecraft.core.Direction.UP)
							&& !level.getBlockState(below).isAir()
							&& level.getRandom().nextFloat() < 0.5f) {
						level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
					}
				}
			}
		}
		for (int i = 0; i < 6; i++) {
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
			if (bolt != null) {
				double angle = i * (Math.PI / 3.0);
				double rd = 5.0 + level.getRandom().nextDouble() * 4.0;
				bolt.moveTo(x + Math.cos(angle) * rd, y, z + Math.sin(angle) * rd);
				bolt.setVisualOnly(true);
				level.addFreshEntity(bolt);
			}
		}
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0f, 0.4f);
		level.playSound(null, x, y, z, randomThunderSound(level), SoundSource.PLAYERS, 3.0f, 0.7f);
	}

	private static SoundEvent randomThunderSound(Level level) {
		int pick = level.getRandom().nextInt(3);
		return switch (pick) {
			case 0 -> ModSounds.LIGHTNING_THUNDER_ANIME;
			case 1 -> ModSounds.LIGHTNING_THUNDER_LOUD;
			default -> SoundEvents.LIGHTNING_BOLT_THUNDER;
		};
	}
}
