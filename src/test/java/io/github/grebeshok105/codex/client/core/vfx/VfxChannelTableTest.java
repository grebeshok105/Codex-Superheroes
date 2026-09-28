package io.github.grebeshok105.codex.client.core.vfx;

import io.github.grebeshok105.codex.core.net.VfxChannelS2CPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link VfxChannelTable} — the Minecraft-free channel addressing and
 * expiry logic. Pinned scenario: an observer that starts tracking an entity
 * mid-channel sees the running effect (UPDATE opens, silence releases).
 */
class VfxChannelTableTest {
	private static final ResourceLocation CHANNEL =
			ResourceLocation.fromNamespaceAndPath("superheroes", "test/channel");
	private static final Vec3 TARGET_A = new Vec3(1, 2, 3);
	private static final Vec3 TARGET_B = new Vec3(4, 5, 6);

	private static final class FakeChannel implements VfxChannelEffect {
		int ticks;
		int retargets;
		boolean released;
		boolean cancelled;
		boolean done;
		Vec3 target;

		@Override
		public void tick() {
			ticks++;
		}

		@Override
		public void render(VfxRenderContext ctx) {
		}

		@Override
		public boolean done() {
			return done;
		}

		@Override
		public void cancel() {
			cancelled = true;
		}

		@Override
		public void retarget(Vec3 target) {
			retargets++;
			this.target = target;
		}

		@Override
		public void release() {
			released = true;
		}
	}

	private static final class Rig {
		final VfxInstanceTable instances = new VfxInstanceTable(8);
		final List<FakeChannel> opened = new ArrayList<>();
		final VfxChannelTable channels;

		Rig() {
			channels = new VfxChannelTable(instances,
					(entityId, channel, target) -> {
						FakeChannel openedChannel = new FakeChannel();
						openedChannel.target = target;
						opened.add(openedChannel);
						return openedChannel;
					},
					VfxRuntime.CHANNEL_TIMEOUT_TICKS);
		}
	}

	@Test
	void updateWithoutStartOpensChannel() {
		Rig rig = new Rig();

		rig.channels.channel(7, CHANNEL, VfxChannelS2CPayload.UPDATE, TARGET_A);

		assertEquals(1, rig.opened.size());
		assertEquals(1, rig.channels.size());
		assertEquals(1, rig.instances.size());
		assertEquals(TARGET_A, rig.opened.get(0).target);
	}

	@Test
	void duplicateStartRetargetsInsteadOfDuplicating() {
		Rig rig = new Rig();

		rig.channels.channel(7, CHANNEL, VfxChannelS2CPayload.START, TARGET_A);
		rig.channels.channel(7, CHANNEL, VfxChannelS2CPayload.START, TARGET_B);

		assertEquals(1, rig.opened.size());
		assertEquals(1, rig.channels.size());
		assertEquals(1, rig.instances.size());
		FakeChannel channel = rig.opened.get(0);
		assertEquals(1, channel.retargets);
		assertEquals(TARGET_B, channel.target);
	}

	@Test
	void stopReleasesAndRemovesWhenDone() {
		Rig rig = new Rig();
		rig.channels.channel(7, CHANNEL, VfxChannelS2CPayload.START, TARGET_A);
		FakeChannel channel = rig.opened.get(0);

		rig.channels.channel(7, CHANNEL, VfxChannelS2CPayload.STOP, TARGET_A);

		assertTrue(channel.released);
		assertEquals(0, rig.channels.size());
		// The channel keeps ticking/rendering through its release phase until done().
		assertEquals(1, rig.instances.size());
		channel.done = true;
		rig.instances.tick();
		assertEquals(0, rig.instances.size());
	}

	@Test
	void channelExpiresWithoutUpdates() {
		Rig rig = new Rig();
		rig.channels.channel(7, CHANNEL, VfxChannelS2CPayload.START, TARGET_A);
		FakeChannel channel = rig.opened.get(0);

		for (int i = 0; i < VfxRuntime.CHANNEL_TIMEOUT_TICKS; i++) {
			rig.channels.tick();
		}
		assertFalse(channel.released);

		rig.channels.tick();

		assertTrue(channel.released);
		assertEquals(0, rig.channels.size());
	}
}
