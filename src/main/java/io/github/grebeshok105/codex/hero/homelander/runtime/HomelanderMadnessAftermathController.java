package io.github.grebeshok105.codex.hero.homelander.runtime;

import io.github.grebeshok105.codex.hero.homelander.effect.HomelanderEffects;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.core.particle.SilentParticles;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.sound.ModSounds;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.mechanic.world.WorldDestructionPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class HomelanderMadnessAftermathController {
	public static final int AFTERMATH_TICKS = 200;
	// ClearOn.LEAVE replaces the offline-player sweep pruneGonePlayers ran every tick.
	private static final OwnedSessionMap<UUID, Boolean> hadMadnessLastTick =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE));

	private HomelanderMadnessAftermathController() {
	}


	private static void triggerAftermath(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(HomelanderEffects.MADNESS_AFTERMATH, AFTERMATH_TICKS, 0, false, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, AFTERMATH_TICKS, 4, false, false, true));
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		HeroData data = HeroDataStore.get(player);
		if (data.hasHero()) {
			for (ResourceLocation abilityId : new HashSet<>(data.activeAbilities())) {
				AbilityRouter.deactivate(player, abilityId);
			}
		}
		VfxFx.eventAround(player.serverLevel(), HomelanderVfxIds.SUN_CHARGE,
				player.position(), player.position(), 1f, 96.0);
	}

	private static void tickAftermath(ServerPlayer player) {
		MobEffectInstance effect = player.getEffect(HomelanderEffects.MADNESS_AFTERMATH);
		if (effect == null) {
			return;
		}
		int remaining = effect.getDuration();
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		player.fallDistance = 0f;
		player.resetFallDistance();
		if (remaining <= 1) {
			detonateSun(player);
		}
	}

	private static void detonateSun(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		VfxFx.eventAround(level, HomelanderVfxIds.SUN_DETONATION,
				new Vec3(x, y, z), new Vec3(x, y, z), 1f, 160.0);
		// SilentParticles + the silent sound holder mute the vanilla boom — the
		// SUN_DETONATION event is the single owner of homelander.sun.detonate.
		level.explode(player, null, null, x, y, z, 12f, true, Level.ExplosionInteraction.MOB,
				SilentParticles.SILENT, SilentParticles.SILENT,
				BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SILENT));
		level.explode(player, null, null, x, y, z, 7f, true, Level.ExplosionInteraction.MOB,
				SilentParticles.SILENT, SilentParticles.SILENT,
				BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SILENT));
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
						WorldDestructionPolicy.tryPlace(level, pos, Blocks.FIRE.defaultBlockState(), player);
					}
				}
			}
		}
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		boolean madness = HomelanderEffects.isMadness(player);
		if (madness) {
			hadMadnessLastTick.put(player.getUUID(), player.getUUID(), Boolean.TRUE);
		} else if (hadMadnessLastTick.remove(player.getUUID()) != null) {
			triggerAftermath(player);
		}
		if (HomelanderEffects.isAftermath(player)) {
			tickAftermath(player);
		}
	}

}
