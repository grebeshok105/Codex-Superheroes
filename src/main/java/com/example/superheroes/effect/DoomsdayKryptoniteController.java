package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.item.KryptoniteShardItem;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DoomsdayKryptoniteController {
	private static final float SHARD_THRESHOLD = 30.0f;
	private static final int MIN_TIER = 4;
	private static final int CLEANSE_TICK_INTERVAL = 20;
	private static final int CONSUME_COUNT = 3;

	private static final Map<UUID, Map<UUID, Float>> ACCUM = new ConcurrentHashMap<>();

	private DoomsdayKryptoniteController() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer doomsday)) return true;
			if (!isDoomsday(doomsday)) return true;
			DoomsdayProgress progress = doomsday.getAttachedOrCreate(ModAttachments.DOOMSDAY_PROGRESS);
			if (progress.tier() < MIN_TIER) return true;

			Entity attackerEntity = source.getEntity();
			if (!(attackerEntity instanceof ServerPlayer attacker)) return true;
			if (attacker.getUUID().equals(doomsday.getUUID())) return true;

			if (DoomsdayAdaptationController.wouldBlock(doomsday, source)) return true;
			if (amount <= 0f) return true;

			Map<UUID, Float> perAttacker = ACCUM.computeIfAbsent(doomsday.getUUID(), k -> new HashMap<>());
			float total = perAttacker.merge(attacker.getUUID(), amount, Float::sum);
			if (total >= SHARD_THRESHOLD) {
				int shardsToDrop = (int) (total / SHARD_THRESHOLD);
				float remainder = total - shardsToDrop * SHARD_THRESHOLD;
				perAttacker.put(attacker.getUUID(), remainder);
				for (int i = 0; i < shardsToDrop; i++) {
					dropShard(doomsday, attacker);
				}
			}
			return true;
		});

		ServerTickEvents.END_SERVER_TICK.register(DoomsdayKryptoniteController::serverTick);
	}

	private static void dropShard(ServerPlayer doomsday, ServerPlayer attacker) {
		ServerLevel level = doomsday.serverLevel();
		Vec3 pos = doomsday.position().add(0, 1.2, 0);
		ItemStack stack = KryptoniteShardItem.create(attacker.getUUID(), doomsday.getUUID());
		ItemEntity ie = new ItemEntity(level, pos.x, pos.y, pos.z, stack);
		ie.setDeltaMovement(level.random.nextGaussian() * 0.1, 0.35, level.random.nextGaussian() * 0.1);
		ie.setPickUpDelay(10);
		level.addFreshEntity(ie);
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ELDER_GUARDIAN_HURT, SoundSource.PLAYERS, 0.6f, 1.6f);

		attacker.displayClientMessage(
				Component.translatable("hero.superheroes.doomsday.kryptonite.drop")
						.withStyle(ChatFormatting.GREEN), true);
	}

	private static void serverTick(MinecraftServer server) {
		if (server.getTickCount() % CLEANSE_TICK_INTERVAL != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (isDoomsday(player)) continue;
			tryAutoCleanse(server, player);
		}
	}

	private static void tryAutoCleanse(MinecraftServer server, ServerPlayer attacker) {
		Map<UUID, Integer> shardCountByDoomsday = new HashMap<>();
		Map<UUID, Integer> firstSlot = new HashMap<>();
		for (int slot = 0; slot < attacker.getInventory().getContainerSize(); slot++) {
			ItemStack stack = attacker.getInventory().getItem(slot);
			if (stack.isEmpty() || !stack.is(ModItems.KRYPTONITE_SHARD)) continue;
			UUID target = KryptoniteShardItem.getTargetDoomsday(stack);
			if (target == null) continue;
			UUID owner = KryptoniteShardItem.getOwner(stack);
			if (owner == null || !owner.equals(attacker.getUUID())) continue;
			shardCountByDoomsday.merge(target, stack.getCount(), Integer::sum);
			firstSlot.putIfAbsent(target, slot);
		}

		for (Map.Entry<UUID, Integer> entry : shardCountByDoomsday.entrySet()) {
			if (entry.getValue() < CONSUME_COUNT) continue;
			UUID doomsdayUuid = entry.getKey();
			ServerPlayer doomsday = findDoomsdayByUuid(server, doomsdayUuid);
			if (doomsday == null) continue;

			ResourceKey<DamageType> stripped = DoomsdayAdaptationController.stripOneAdaptation(doomsday);
			if (stripped == null) continue;

			consumeShards(attacker, doomsdayUuid, CONSUME_COUNT);

			ServerLevel level = attacker.serverLevel();
			level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
					SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2f, 1.0f);
			level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
					SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
			ServerLevel doomsdayLevel = doomsday.serverLevel();
			doomsdayLevel.playSound(null, doomsday.getX(), doomsday.getY(), doomsday.getZ(),
					SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 1.4f, 0.6f);

			String typePath = stripped.location().getPath();
			attacker.displayClientMessage(
					Component.translatable("hero.superheroes.doomsday.kryptonite.cleansed",
							Component.literal(typePath).withStyle(ChatFormatting.GREEN))
							.withStyle(ChatFormatting.AQUA), true);
			doomsday.displayClientMessage(
					Component.translatable("hero.superheroes.doomsday.kryptonite.lost",
							Component.literal(typePath).withStyle(ChatFormatting.RED))
							.withStyle(ChatFormatting.DARK_RED), false);
		}
	}

	private static void consumeShards(ServerPlayer attacker, UUID doomsdayUuid, int count) {
		int remaining = count;
		for (int slot = 0; slot < attacker.getInventory().getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = attacker.getInventory().getItem(slot);
			if (stack.isEmpty() || !stack.is(ModItems.KRYPTONITE_SHARD)) continue;
			UUID target = KryptoniteShardItem.getTargetDoomsday(stack);
			if (target == null || !target.equals(doomsdayUuid)) continue;
			int take = Math.min(stack.getCount(), remaining);
			stack.shrink(take);
			remaining -= take;
		}
		attacker.getInventory().setChanged();
	}

	private static ServerPlayer findDoomsdayByUuid(MinecraftServer server, UUID uuid) {
		ServerPlayer p = server.getPlayerList().getPlayer(uuid);
		if (p == null) return null;
		if (!isDoomsday(p)) return null;
		return p;
	}

	private static boolean isDoomsday(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && DoomsdayHero.ID.equals(data.heroId());
	}
}
