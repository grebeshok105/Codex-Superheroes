package io.github.grebeshok105.codex.gametest;

import com.mojang.authlib.GameProfile;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.net.VfxChannelS2CPayload;
import io.github.grebeshok105.codex.hero.homelander.HomelanderAbilityIds;
import io.github.grebeshok105.codex.hero.homelander.HomelanderHero;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Task 9: the eye lasers broadcast a {@code VfxChannelS2CPayload} stream on
 * {@code superheroes:homelander/laser} — START on activate, UPDATE heartbeats
 * every {@code VfxFx.CHANNEL_UPDATE_INTERVAL_TICKS} ticks (including the
 * uranium-pulse {@code fire=false} pauses), STOP on deactivate. Damage keeps
 * landing from the first active tick; the channel is presentation-only.
 */
public final class HomelanderVfxGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void eyeLaserBroadcastsStartUpdateStop(GameTestHelper helper) {
		Wire homelander = joinAudible(helper, "laser-homelander");
		TestHeroes.transform(homelander.player(), HomelanderHero.ID);
		Zombie zombie = spawnAhead(helper, homelander.player());
		TestPlayers.awaitVisible(helper, zombie, () -> {
			float hp = zombie.getHealth();
			drain(homelander.channel());
			AbilityRouter.activate(homelander.player(), HomelanderAbilityIds.EYE_LASERS);
			helper.runAfterDelay(10, () -> {
				AbilityRouter.deactivate(homelander.player(), HomelanderAbilityIds.EYE_LASERS);
				List<VfxChannelS2CPayload> laser = new ArrayList<>();
				// Payloads queue on the connection's event loop: poll while
				// accumulating until the STOP lands (or the budget runs out).
				await(helper, () -> {
					laser.addAll(laserChannelPackets(
							drain(homelander.channel()), homelander.player().getId()));
					return !laser.isEmpty()
							&& laser.get(laser.size() - 1).state() == VfxChannelS2CPayload.STOP;
				}, 20, () -> {
					helper.assertTrue(!laser.isEmpty(),
							"eye lasers broadcast a laser channel stream, got none");
					helper.assertTrue(laser.get(0).state() == VfxChannelS2CPayload.START,
							"first laser packet is START, got states " + states(laser));
					long updates = laser.stream()
							.filter(p -> p.state() == VfxChannelS2CPayload.UPDATE).count();
					helper.assertTrue(updates >= 3,
							"at least 3 UPDATE heartbeats in 10 active ticks, got " + updates);
					helper.assertTrue(laser.get(laser.size() - 1).state() == VfxChannelS2CPayload.STOP,
							"last laser packet is STOP, got states " + states(laser));
					helper.assertTrue(zombie.getHealth() < hp,
							"laser damage lands from the first active tick");
					TestPlayers.leave(homelander.player());
					helper.succeed();
				});
			});
		});
	}

	// ---- helpers ------------------------------------------------------------

	/** A joined player whose raw outbound packets stay readable for assertions. */
	private record Wire(ServerPlayer player, EmbeddedChannel channel) {
	}

	private static Wire joinAudible(GameTestHelper helper, String name) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), name);
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				profile, cookie.clientInformation());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		EmbeddedChannel channel = new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		return new Wire(player, channel);
	}

	/** Retries {@code ready} once per tick until it holds; drains may accumulate packets. */
	private static void await(GameTestHelper helper, BooleanSupplier ready, int tries, Runnable done) {
		if (ready.getAsBoolean()) {
			done.run();
			return;
		}
		helper.assertTrue(tries > 0, "condition never became true");
		helper.runAfterDelay(1, () -> await(helper, ready, tries - 1, done));
	}

	private static List<Object> drain(EmbeddedChannel channel) {
		channel.runPendingTasks();
		List<Object> out = new ArrayList<>();
		for (Object o = channel.readOutbound(); o != null; o = channel.readOutbound()) {
			out.add(o);
		}
		return out;
	}

	private static List<VfxChannelS2CPayload> laserChannelPackets(List<Object> packets, int entityId) {
		List<VfxChannelS2CPayload> out = new ArrayList<>();
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof VfxChannelS2CPayload channel
					&& channel.channel().equals(HomelanderVfxIds.LASER)
					&& channel.entityId() == entityId) {
				out.add(channel);
			}
		}
		return out;
	}

	private static String states(List<VfxChannelS2CPayload> laser) {
		StringBuilder sb = new StringBuilder("[");
		for (VfxChannelS2CPayload p : laser) {
			sb.append(p.state()).append(',');
		}
		return sb.append(']').toString();
	}

	private static Zombie spawnAhead(GameTestHelper helper, ServerPlayer player) {
		Vec3 ahead = player.position().add(player.getViewVector(1f).normalize().scale(2.0));
		helper.getLevel().getChunk(BlockPos.containing(ahead.x, player.getY(), ahead.z));
		Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
		zombie.moveTo(ahead.x, player.getY(), ahead.z, 0f, 0f);
		helper.getLevel().addFreshEntity(zombie);
		return zombie;
	}
}
