package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.hero.HeroAttributes;
import com.example.superheroes.network.DoomsdayProgressS2CPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class DoomsdayTierController {
	private static final int RELOCATE_MIN = 50;
	private static final int RELOCATE_MAX = 100;

	private DoomsdayTierController() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof ServerPlayer player)) return;
			if (!isDoomsday(player)) return;
			handleDeath(player, source);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!isDoomsday(newPlayer)) return;
			handleRespawn(newPlayer);
		});
	}

	private static void handleDeath(ServerPlayer player, DamageSource source) {
		DoomsdayProgress progress = player.getAttachedOrCreate(ModAttachments.DOOMSDAY_PROGRESS);
		String sourceKey = describeDamageSource(source);
		Vec3 deathPos = player.position();

		int oldTier = progress.tier();

		DoomsdayProgress next = progress.withDeathSource(sourceKey, deathPos);
		if (oldTier < 7) {
			next = next.withTierUp(player.serverLevel().getGameTime());
		}
		player.setAttached(ModAttachments.DOOMSDAY_PROGRESS, next);

		// Permanent immunity to that damage type via existing adaptation system
		ResourceKey<DamageType> typeKey = source.typeHolder().unwrapKey().orElse(null);
		if (typeKey != null) {
			DoomsdayAdaptationController.registerAdaptation(player, typeKey, true);
		}

		broadcastDeath(player, sourceKey, oldTier, next.tier());
	}

	private static void handleRespawn(ServerPlayer player) {
		DoomsdayProgress progress = player.getAttachedOrCreate(ModAttachments.DOOMSDAY_PROGRESS);

		// Player entity was reconstructed on respawn; tickCount reset to 0.
		// Any tick-based cooldowns keyed by UUID are now stuck at far-future deadlines.
		// Clear them so abilities work again.
		java.util.UUID id = player.getUUID();
		com.example.superheroes.ability.AbilityCooldowns.clear(id);
		SuperJumpController.clear(id);
		FlightController.clear(id);
		DoomGripController.clear(player);
		com.example.superheroes.ability.ChargeTackleAbility.clear(player);

		applyProgress(player);

		if (progress.pendingRelocate()) {
			Vec3 deathPos = progress.lastDeathPos();
			ServerLevel level = player.serverLevel();
			tryRelocate(player, level, deathPos);
			player.setAttached(ModAttachments.DOOMSDAY_PROGRESS, progress.withRelocateClear());
		}

		sync(player);
	}

	public static void applyProgress(ServerPlayer player) {
		DoomsdayProgress progress = player.getAttachedOrCreate(ModAttachments.DOOMSDAY_PROGRESS);
		HeroAttributes.DOOMSDAY.remove(player);
		HeroAttributes.buildDoomsdayTierSet(progress.tier()).apply(player);
		DoomsdayHero.applyTierEffects(player, progress.tier());
		DoomsdayAdaptationController.reapplyDamageBonus(player);
		player.setHealth(player.getMaxHealth());
		sync(player);
	}

	public static void setTier(ServerPlayer player, int tier) {
		int clamped = Math.max(1, Math.min(7, tier));
		DoomsdayProgress progress = player.getAttachedOrCreate(ModAttachments.DOOMSDAY_PROGRESS);
		player.setAttached(ModAttachments.DOOMSDAY_PROGRESS, progress.withTier(clamped));
		applyProgress(player);
	}

	private static void tryRelocate(ServerPlayer player, ServerLevel level, Vec3 from) {
		java.util.Random rng = level.getRandom() instanceof java.util.Random r ? r : new java.util.Random();
		for (int attempt = 0; attempt < 16; attempt++) {
			double angle = rng.nextDouble() * Math.PI * 2.0;
			double dist = RELOCATE_MIN + rng.nextDouble() * (RELOCATE_MAX - RELOCATE_MIN);
			int tx = (int) Math.floor(from.x + Math.cos(angle) * dist);
			int tz = (int) Math.floor(from.z + Math.sin(angle) * dist);
			int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz);
			if (ty <= level.getMinBuildHeight() + 1) continue;
			BlockPos pos = new BlockPos(tx, ty, tz);
			if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
					&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
				player.teleportTo(level, tx + 0.5, ty, tz + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot());
				player.connection.resetPosition();
				return;
			}
		}
	}

	private static void broadcastDeath(ServerPlayer player, String source, int oldTier, int newTier) {
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.4f, 0.7f);

		Component msg;
		if (newTier > oldTier) {
			msg = Component.translatable("hero.superheroes.doomsday.death.evolve",
					player.getName(),
					Component.literal(source).withStyle(ChatFormatting.RED),
					Component.literal(String.valueOf(newTier)).withStyle(ChatFormatting.LIGHT_PURPLE))
					.withStyle(ChatFormatting.GRAY);
		} else {
			msg = Component.translatable("hero.superheroes.doomsday.death.simple",
					player.getName(),
					Component.literal(source).withStyle(ChatFormatting.RED))
					.withStyle(ChatFormatting.GRAY);
		}
		if (player.getServer() != null) {
			player.getServer().getPlayerList().broadcastSystemMessage(msg, false);
		}
	}

	public static String describeDamageSource(DamageSource source) {
		String typeKey = source.typeHolder().unwrapKey()
				.map(k -> k.location().toString())
				.orElseGet(() -> source.type().msgId());
		if (source.getEntity() instanceof ServerPlayer attacker) {
			HeroData attackerHero = attacker.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (attackerHero.hasHero()) {
				return typeKey + "@" + attackerHero.heroId().getPath();
			}
		}
		return typeKey;
	}

	public static void sync(ServerPlayer player) {
		DoomsdayProgress progress = player.getAttachedOrCreate(ModAttachments.DOOMSDAY_PROGRESS);
		ServerPlayNetworking.send(player, new DoomsdayProgressS2CPayload(progress.tier(), progress.adaptations()));
	}

	public static void resetProgress(ServerPlayer player) {
		player.setAttached(ModAttachments.DOOMSDAY_PROGRESS, DoomsdayProgress.EMPTY);
		HeroAttributes.DOOMSDAY.remove(player);
		sync(player);
	}

	private static boolean isDoomsday(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && DoomsdayHero.ID.equals(data.heroId());
	}
}
