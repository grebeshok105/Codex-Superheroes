package io.github.grebeshok105.codex.gametest;

import com.mojang.authlib.GameProfile;
import io.github.grebeshok105.codex.core.net.VfxEventS2CPayload;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Task 13: the {@code /superheroes vfx} showcase commands. A scene broadcast
 * reaches every nearby client (the observer at 20 blocks sees the same
 * sun_detonation event as the caller), and {@code stress} refuses counts
 * above the 64 cap — error feedback, zero packets.
 */
public final class VfxShowcaseGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void showcaseSceneBroadcastsToNearbyObservers(GameTestHelper helper) {
		Wire actor = joinAudible(helper, "vfx-actor");
		Wire observer = joinAudible(helper, "vfx-observer");
		observer.player().moveTo(actor.player().getX() + 20.0,
				actor.player().getY(), actor.player().getZ(), 0f, 0f);

		drain(actor.channel());
		drain(observer.channel());
		runAs(actor.player(), 2, "superheroes vfx scene superheroes:homelander/sun_detonation");

		List<VfxEventS2CPayload> actorEvents = new ArrayList<>();
		List<VfxEventS2CPayload> observerEvents = new ArrayList<>();
		await(helper, () -> {
			actorEvents.addAll(vfxEvents(drain(actor.channel()), HomelanderVfxIds.SUN_DETONATION));
			observerEvents.addAll(vfxEvents(drain(observer.channel()), HomelanderVfxIds.SUN_DETONATION));
			return !actorEvents.isEmpty() && !observerEvents.isEmpty();
		}, 20, () -> {
			helper.assertTrue(!actorEvents.isEmpty(),
					"the caller's own client must see the sun_detonation event");
			helper.assertTrue(!observerEvents.isEmpty(),
					"an observer 20 blocks away must see the sun_detonation event");
			TestPlayers.leave(actor.player());
			TestPlayers.leave(observer.player());
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void stressCountIsCapped(GameTestHelper helper) {
		Wire player = joinAudible(helper, "vfx-stress");
		drain(player.channel());

		List<Component> messages = runAs(player.player(), 2, "superheroes vfx stress 1000");

		helper.assertTrue(hasKey(messages, "commands.superheroes.vfx.stress.too_many"),
				"stress 1000 must report vfx.stress.too_many, got " + messages);
		// Nothing may reach the wire — not even a partial burst. Draining a few
		// ticks later makes an illegal send observable rather than racy.
		helper.runAfterDelay(5, () -> {
			List<VfxEventS2CPayload> sent = vfxEvents(drain(player.channel()), null);
			helper.assertTrue(sent.isEmpty(),
					"stress over the cap must send no vfx events, got " + sent.size());
			TestPlayers.leave(player.player());
			helper.succeed();
		});
	}

	// ---- helpers ------------------------------------------------------------

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

	private static List<VfxEventS2CPayload> vfxEvents(List<Object> packets, ResourceLocation effect) {
		List<VfxEventS2CPayload> out = new ArrayList<>();
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof VfxEventS2CPayload event
					&& (effect == null || event.effect().equals(effect))) {
				out.add(event);
			}
		}
		return out;
	}

	/**
	 * Runs {@code command} through the real dispatcher as if {@code player} had typed it
	 * at the given permission level; returns every feedback message the command sent.
	 */
	private static List<Component> runAs(ServerPlayer player, int permission, String command) {
		List<Component> messages = new ArrayList<>();
		CommandSource capture = new CommandSource() {
			@Override
			public void sendSystemMessage(Component message) {
				messages.add(message);
			}

			@Override
			public boolean acceptsSuccess() {
				return true;
			}

			@Override
			public boolean acceptsFailure() {
				return true;
			}

			@Override
			public boolean shouldInformAdmins() {
				return true;
			}
		};
		ServerLevel level = player.serverLevel();
		CommandSourceStack stack = player.createCommandSourceStack()
				.withSource(capture)
				.withPermission(permission);
		level.getServer().getCommands().performPrefixedCommand(stack, command);
		return messages;
	}

	private static boolean hasKey(List<Component> messages, String key) {
		for (Component message : messages) {
			if (containsKey(message, key)) {
				return true;
			}
		}
		return false;
	}

	// Error feedback arrives as Component.empty().withStyle(RED).append(translatable) —
	// the key lives in the siblings, not the root contents.
	private static boolean containsKey(Component component, String key) {
		if (component.getContents() instanceof TranslatableContents t && key.equals(t.getKey())) {
			return true;
		}
		for (Component sibling : component.getSiblings()) {
			if (containsKey(sibling, key)) {
				return true;
			}
		}
		return false;
	}
}
