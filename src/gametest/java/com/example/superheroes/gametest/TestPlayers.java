package com.example.superheroes.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Real {@link ServerPlayer}s joined through the player list, so join/leave hooks run as in play.
 * They are not ticked by a connection: tests drive inventory ticks explicitly.
 */
final class TestPlayers {
	private TestPlayers() {
	}

	static ServerPlayer join(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		return player;
	}

	/** Runs the same disconnect path a closing client triggers on the server thread. */
	static void leave(ServerPlayer player) {
		player.connection.onDisconnect(new DisconnectionDetails(Component.literal("gametest")));
	}

	/**
	 * Mock players join with a private {@code spawnInvulnerableTime=60} that makes {@code hurt()}
	 * return false for damage not in {@code #bypasses_invulnerability}; {@code tick()} only
	 * decrements it by 1. Clear it when a test needs real damage to land.
	 */
	static void clearSpawnInvulnerability(ServerPlayer player) {
		try {
			java.lang.reflect.Field f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			f.setAccessible(true);
			f.setInt(player, 0);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		player.invulnerableTime = 0;
	}

	static void fillInventory(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.items.size(); i++) {
			inventory.items.set(i, new ItemStack(Items.DIRT));
		}
		inventory.offhand.set(0, new ItemStack(Items.DIRT));
	}

	static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
		int n = 0;
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(item)) {
				n += inventory.getItem(i).getCount();
			}
		}
		if (player.containerMenu.getCarried().is(item)) {
			n += player.containerMenu.getCarried().getCount();
		}
		return n;
	}
}
