package io.github.grebeshok105.codex.gametest;

import com.mojang.authlib.GameProfile;
import io.github.grebeshok105.codex.core.net.ScorchMark;
import io.github.grebeshok105.codex.core.net.ScorchMarksS2CPayload;
import io.github.grebeshok105.codex.hero.homelander.scorch.LaserScorchData;
import io.github.grebeshok105.codex.hero.homelander.scorch.LaserScorchSync;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stage 12 pins for persistent laser scorch marks: the mark is admitted only on
 * a sturdy face, the {@link LaserScorchData} SavedData survives a save/load
 * round-trip with its marks intact, the dimension sync streams every mark in
 * {@link ScorchMarksS2CPayload#SYNC_CHUNK}-sized reset-flagged chunks, and a
 * newly stamped mark is broadcast as an append (not a reset).
 */
public final class HomelanderScorchGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void tryAddStampsOnSturdyFaceOnly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos wall = helper.absolutePos(new BlockPos(1, 1, 1));
		level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
		LaserScorchData data = LaserScorchData.get(level);
		UUID caster = UUID.randomUUID();

		ScorchMark mark = data.tryAdd(level, caster, new BlockHitResult(
				Vec3.atCenterOf(wall).add(0, 0.5, 0), Direction.UP, wall, false));
		helper.assertTrue(mark != null, "mark stamped on a sturdy face");
		helper.assertTrue(mark.pos().equals(wall), "mark anchored to the hit block");
		helper.assertTrue(mark.face() == (byte) Direction.UP.get3DDataValue(), "mark records the hit face");
		helper.assertTrue(mark.size() >= 0.25f && mark.size() <= 0.4f,
				"mark size inside 0.25..0.4, got " + mark.size());

		// a face over air is not sturdy — no mark lands
		ScorchMark rejected = data.tryAdd(level, caster, new BlockHitResult(
				Vec3.atCenterOf(wall).add(5, 3, 0), Direction.UP, wall.above(3), false));
		helper.assertTrue(rejected == null, "no mark on a non-sturdy face");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void marksSurviveSavedDataRoundTrip(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos floor = helper.absolutePos(new BlockPos(2, 1, 2));
		level.setBlockAndUpdate(floor, Blocks.DEEPSLATE.defaultBlockState());
		LaserScorchData data = LaserScorchData.get(level);
		UUID caster = UUID.randomUUID();

		ScorchMark mark = data.tryAdd(level, caster, new BlockHitResult(
				Vec3.atCenterOf(floor).add(0.1, 0.5, -0.2), Direction.UP, floor, false));
		helper.assertTrue(mark != null, "mark stamped");

		CompoundTag tag = data.save(new CompoundTag(), level.registryAccess());
		LaserScorchData restored = LaserScorchData.load(tag, level.registryAccess());
		helper.assertTrue(restored.size() == data.size(),
				"save/load keeps every mark, " + restored.size() + " != " + data.size());
		boolean[] found = {false};
		restored.forEachChronological(m -> {
			if (m.equals(mark)) {
				found[0] = true;
			}
		});
		helper.assertTrue(found[0], "the stamped mark survives the SavedData round-trip");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void dimensionSyncStreamsResetFlaggedChunks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LaserScorchData data = LaserScorchData.get(level);
		// fill past one chunk so the chunked streaming is actually exercised;
		// the SavedData persists across gametests, so the store may already hold
		// earlier marks — assertions count what is streamed, not a fresh total.
		int want = ScorchMarksS2CPayload.SYNC_CHUNK + 8;
		for (int i = 0; i < want; i++) {
			data.insert(new ScorchMark(new BlockPos(100000 + i * 4, 80, -100000),
					(byte) Direction.UP.get3DDataValue(), 0.5f, 0.5f, 0.3f, i));
		}
		Wire viewer = joinViewer(helper, "scorch_viewer");
		drain(viewer.channel()); // drop the JOIN-triggered sync
		LaserScorchSync.sendAll(viewer.player());

		List<ScorchMarksS2CPayload> payloads = scorchPayloads(drain(viewer.channel()));
		helper.assertTrue(payloads.size() >= 2, "marks stream in more than one chunk, got " + payloads.size());
		helper.assertTrue(payloads.get(0).reset(), "first sync chunk carries the reset flag");
		helper.assertTrue(payloads.get(0).marks().size() == ScorchMarksS2CPayload.SYNC_CHUNK,
				"first chunk is exactly 256 marks, got " + payloads.get(0).marks().size());
		for (int i = 1; i < payloads.size(); i++) {
			helper.assertTrue(!payloads.get(i).reset(), "later chunk " + i + " is an append");
			helper.assertTrue(payloads.get(i).marks().size() <= ScorchMarksS2CPayload.SYNC_CHUNK,
					"chunk " + i + " within the 256 bound");
		}
		long total = payloads.stream().mapToLong(p -> p.marks().size()).sum();
		helper.assertTrue(total >= want, "every stored mark reaches the viewer, got " + total);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void newMarkBroadcastsAsAppend(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos spot = helper.absolutePos(new BlockPos(3, 1, 3));
		level.setBlockAndUpdate(spot, Blocks.STONE.defaultBlockState());
		Wire viewer = joinViewer(helper, "scorch_watcher");
		drain(viewer.channel()); // drop the JOIN-triggered sync
		// put the viewer next to the mark so it is inside the 128-block radius
		viewer.player().teleportTo(spot.getX() + 2, spot.getY() + 2, spot.getZ());

		ScorchMark mark = new ScorchMark(spot, (byte) Direction.UP.get3DDataValue(),
				0.5f, 0.5f, 0.3f, 0);
		LaserScorchSync.broadcastNew(level, mark);
		List<ScorchMarksS2CPayload> payloads = scorchPayloads(drain(viewer.channel()));
		helper.assertTrue(payloads.size() == 1, "exactly one scorch packet, got " + payloads.size());
		helper.assertTrue(!payloads.get(0).reset(), "broadcast is an append, not a reset");
		helper.assertTrue(payloads.get(0).marks().equals(List.of(mark)), "broadcast carries the new mark");
		helper.succeed();
	}

	// ---- helpers -----------------------------------------------------------

	private record Wire(ServerPlayer player, EmbeddedChannel channel) {
	}

	private static Wire joinViewer(GameTestHelper helper, String name) {
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
		// see BeamPayloadGameTests#drain: runPendingTasks() flushes queued writes.
		channel.runPendingTasks();
		List<Object> out = new ArrayList<>();
		for (Object o = channel.readOutbound(); o != null; o = channel.readOutbound()) {
			out.add(o);
		}
		return out;
	}

	private static List<ScorchMarksS2CPayload> scorchPayloads(List<Object> packets) {
		List<ScorchMarksS2CPayload> out = new ArrayList<>();
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof ScorchMarksS2CPayload payload) {
				out.add(payload);
			}
		}
		return out;
	}
}
