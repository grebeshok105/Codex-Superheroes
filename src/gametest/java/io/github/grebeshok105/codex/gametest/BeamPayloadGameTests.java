package io.github.grebeshok105.codex.gametest;

import com.mojang.authlib.GameProfile;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.net.BeamFx;
import io.github.grebeshok105.codex.core.net.BeamFxS2CPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stage L2 "after" pins: the three beam payloads merged into
 * {@link BeamFxS2CPayload} under the single id {@code superheroes:beam_fx} with
 * a style field (ResourceLocation + UUID + 2xVec3 on the wire), and the
 * broadcast audiences keep the deliberate asymmetry — {@link BeamFx#laser}
 * uses {@code FxBroadcast.tracking} (the shooter is NOT in it: the local
 * overlay renders the shooter's own beam), while {@link BeamFx#repulsor} and
 * {@link BeamFx#cosmicBeam} use {@code trackingAndSelf}.
 */
public final class BeamPayloadGameTests implements FabricGameTest {
	private static final UUID SHOOTER = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
	private static final Vec3 START = new Vec3(1.5, 66.25, -4.0);
	private static final Vec3 END = new Vec3(40.0, 64.0, 7.5);

	@GameTest(template = EMPTY_STRUCTURE)
	public void beamFxPayloadIdAndStyleConstantsAreTheNewContract(GameTestHelper helper) {
		helper.assertTrue(BeamFxS2CPayload.TYPE.id().equals(ModId.of("beam_fx")),
				"unified beam payload id");
		helper.assertTrue(BeamFxS2CPayload.STYLE_LASER.equals(ModId.of("laser")),
				"laser style id");
		helper.assertTrue(BeamFxS2CPayload.STYLE_REPULSOR.equals(ModId.of("repulsor")),
				"repulsor style id");
		helper.assertTrue(BeamFxS2CPayload.STYLE_COSMIC_BEAM.equals(ModId.of("cosmic_beam")),
				"cosmic beam style id");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void beamFxPayloadRoundTripsStyleShooterStartEnd(GameTestHelper helper) {
		ByteBuf buf = Unpooled.buffer();
		try {
			for (ResourceLocation style : List.of(BeamFxS2CPayload.STYLE_LASER,
					BeamFxS2CPayload.STYLE_REPULSOR, BeamFxS2CPayload.STYLE_COSMIC_BEAM)) {
				buf.clear();
				BeamFxS2CPayload beam = new BeamFxS2CPayload(style, SHOOTER, START, END);
				BeamFxS2CPayload.STREAM_CODEC.encode(buf, beam);
				helper.assertTrue(buf.writerIndex() > 64,
						style + " frame is style id + uuid (16) + two vec3s (2 x 24), got " + buf.writerIndex());
				BeamFxS2CPayload dec = BeamFxS2CPayload.STREAM_CODEC.decode(buf);
				helper.assertTrue(dec.style().equals(style), style + " style survives");
				assertBeam(helper, dec.shooter(), dec.start(), dec.end(), style.toString());
			}
		} finally {
			buf.release();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void beamBroadcastAudiencesKeepTheAsymmetry(GameTestHelper helper) {
		Wire shooter = joinAudible(helper, "i6-beam");
		helper.runAfterDelay(2, () -> {
			BeamFx.repulsor(shooter.player(), START, END);
			BeamFx.cosmicBeam(shooter.player(), START, END);
			awaitSelfBeams(helper, shooter, new ArrayList<>(), 15, () -> {
				// Both trackingAndSelf payloads landed. Now the tracking-only send:
				// give the write the same kind of settle window the positives needed,
				// then a single drain — a negative can't poll for absence.
				BeamFx.laser(shooter.player(), START, END);
				helper.runAfterDelay(3, () -> {
					helper.assertTrue(!hasLaser(drain(shooter.channel()), shooter.player().getUUID()),
							"tracking (not self): the laser shooter does NOT get their own payload — "
									+ "the local overlay draws it");
					TestPlayers.leave(shooter.player());
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Polls the wire each tick until the shooter has seen BOTH their own repulsor
	 * and cosmic-beam payloads (or the retry budget runs out). Sends queue on the
	 * embedded channel's event loop, so a single drain can legitimately read empty
	 * even though the packet is in flight; drains consume, so packets accumulate.
	 */
	private static void awaitSelfBeams(GameTestHelper helper, Wire shooter, List<Object> seen,
			int tries, Runnable done) {
		if (tries <= 0) {
			helper.fail("trackingAndSelf beams (repulsor+cosmic) never reached their shooter");
			return;
		}
		seen.addAll(drain(shooter.channel()));
		UUID id = shooter.player().getUUID();
		if (hasBeam(seen, id, BeamFxS2CPayload.STYLE_REPULSOR)
				&& hasBeam(seen, id, BeamFxS2CPayload.STYLE_COSMIC_BEAM)) {
			done.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitSelfBeams(helper, shooter, seen, tries - 1, done));
	}

	// ─────────────────────────── helpers ───────────────────────────

	private static void assertBeam(GameTestHelper helper, UUID shooter, Vec3 start, Vec3 end, String label) {
		helper.assertTrue(shooter.equals(SHOOTER), label + " shooter survives");
		helper.assertTrue(vecEquals(start, START), label + " start survives");
		helper.assertTrue(vecEquals(end, END), label + " end survives");
	}

	private static boolean vecEquals(Vec3 a, Vec3 b) {
		return a.x == b.x && a.y == b.y && a.z == b.z;
	}

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
		player.setGameMode(GameType.SURVIVAL);
		return new Wire(player, channel);
	}

	private static List<Object> drain(EmbeddedChannel channel) {
		// Connection.send dispatches writes via eventLoop().execute() when called off
		// the embedded loop; without running pending tasks the packets sit queued and
		// readOutbound sees nothing — delivery timing is otherwise racy.
		channel.runPendingTasks();
		List<Object> out = new ArrayList<>();
		for (Object o = channel.readOutbound(); o != null; o = channel.readOutbound()) {
			out.add(o);
		}
		return out;
	}

	private static boolean hasBeam(List<Object> packets, UUID shooter, ResourceLocation style) {
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof BeamFxS2CPayload payload
					&& payload.style().equals(style)
					&& payload.shooter().equals(shooter)
					&& vecEquals(payload.start(), START) && vecEquals(payload.end(), END)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasLaser(List<Object> packets, UUID shooter) {
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof BeamFxS2CPayload payload
					&& payload.style().equals(BeamFxS2CPayload.STYLE_LASER)
					&& payload.shooter().equals(shooter)) {
				return true;
			}
		}
		return false;
	}
}
