package io.github.grebeshok105.codex.gametest;

import com.mojang.authlib.GameProfile;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.network.LaserFiredS2CPayload;
import io.github.grebeshok105.codex.network.ModNetworking;
import io.github.grebeshok105.codex.network.RepulsorBlastS2CPayload;
import io.github.grebeshok105.codex.network.ThanosCosmicBeamS2CPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stage L2 "before" pins: the three beam payloads share the exact same wire shape
 * (UUID shooter, Vec3 start, Vec3 end -> 16 + 24 + 24 bytes) under three different
 * type ids, and the broadcast audiences differ on purpose — Homelander's laser uses
 * {@code FxBroadcast.tracking} (the shooter is NOT in it: the local overlay renders
 * the shooter's own beam), while repulsor and thanos use {@code trackingAndSelf}.
 * Phase 2 merges the three records into {@code core/net/BeamFxS2CPayload} under one id
 * and re-points the emitters; these pins carry the field/audience contract across the
 * merge (visual equivalence is verified separately).
 */
public final class BeamPayloadGameTests implements FabricGameTest {
	private static final UUID SHOOTER = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
	private static final Vec3 START = new Vec3(1.5, 66.25, -4.0);
	private static final Vec3 END = new Vec3(40.0, 64.0, 7.5);

	@GameTest(template = EMPTY_STRUCTURE)
	public void beamPayloadTypeIdsAreTheCurrentContract(GameTestHelper helper) {
		helper.assertTrue(LaserFiredS2CPayload.TYPE.id().equals(ModId.of("laser_fired")),
				"homelander laser id");
		helper.assertTrue(RepulsorBlastS2CPayload.TYPE.id().equals(ModId.of("repulsor_blast")),
				"iron man repulsor id");
		helper.assertTrue(ThanosCosmicBeamS2CPayload.TYPE.id().equals(ModId.of("thanos_cosmic_beam")),
				"thanos cosmic beam id");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void beamPayloadsRoundTripShooterStartEnd(GameTestHelper helper) {
		ByteBuf buf = Unpooled.buffer();
		try {
			LaserFiredS2CPayload laser = new LaserFiredS2CPayload(SHOOTER, START, END);
			LaserFiredS2CPayload.STREAM_CODEC.encode(buf, laser);
			helper.assertTrue(buf.writerIndex() == 64,
					"uuid (16) + two vec3s (2 x 24) is the whole frame, got " + buf.writerIndex());
			LaserFiredS2CPayload laserDec = LaserFiredS2CPayload.STREAM_CODEC.decode(buf);
			assertBeam(helper, laserDec.shooter(), laserDec.start(), laserDec.end(), "laser_fired");

			buf.clear();
			RepulsorBlastS2CPayload repulsor = new RepulsorBlastS2CPayload(SHOOTER, START, END);
			RepulsorBlastS2CPayload.STREAM_CODEC.encode(buf, repulsor);
			helper.assertTrue(buf.writerIndex() == 64, "repulsor frame is 64 bytes");
			RepulsorBlastS2CPayload repulsorDec = RepulsorBlastS2CPayload.STREAM_CODEC.decode(buf);
			assertBeam(helper, repulsorDec.shooter(), repulsorDec.start(), repulsorDec.end(), "repulsor_blast");

			buf.clear();
			ThanosCosmicBeamS2CPayload thanos = new ThanosCosmicBeamS2CPayload(SHOOTER, START, END);
			ThanosCosmicBeamS2CPayload.STREAM_CODEC.encode(buf, thanos);
			helper.assertTrue(buf.writerIndex() == 64, "thanos frame is 64 bytes");
			ThanosCosmicBeamS2CPayload thanosDec = ThanosCosmicBeamS2CPayload.STREAM_CODEC.decode(buf);
			assertBeam(helper, thanosDec.shooter(), thanosDec.start(), thanosDec.end(), "thanos_cosmic_beam");
		} finally {
			buf.release();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void beamBroadcastAudiencesKeepTheAsymmetry(GameTestHelper helper) {
		Wire shooter = joinAudible(helper, "i6-beam");
		helper.runAfterDelay(2, () -> {
			ModNetworking.broadcastRepulsor(shooter.player(), START, END);
			ModNetworking.broadcastThanosCosmicBeam(shooter.player(), START, END);
			List<Object> packets = drain(shooter.channel());
			helper.assertTrue(hasRepulsor(packets, shooter.player().getUUID()),
					"trackingAndSelf: the repulsor shooter sees their own beam");
			helper.assertTrue(hasThanosBeam(packets, shooter.player().getUUID()),
					"trackingAndSelf: the thanos shooter sees their own beam");

			ModNetworking.broadcastLaser(shooter.player(), START, END);
			helper.assertTrue(!hasLaser(drain(shooter.channel()), shooter.player().getUUID()),
					"tracking (not self): the laser shooter does NOT get their own payload — "
							+ "the local overlay draws it");
			TestPlayers.leave(shooter.player());
			helper.succeed();
		});
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

	private static boolean hasRepulsor(List<Object> packets, UUID shooter) {
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof RepulsorBlastS2CPayload payload
					&& payload.shooter().equals(shooter)
					&& vecEquals(payload.start(), START) && vecEquals(payload.end(), END)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasThanosBeam(List<Object> packets, UUID shooter) {
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof ThanosCosmicBeamS2CPayload payload
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
					&& custom.payload() instanceof LaserFiredS2CPayload payload
					&& payload.shooter().equals(shooter)) {
				return true;
			}
		}
		return false;
	}
}
