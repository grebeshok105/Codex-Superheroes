package io.github.grebeshok105.codex.client.core.vfx;

import io.github.grebeshok105.codex.core.net.VfxChannelS2CPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The channel index behind {@link VfxRuntime}: maps {@code (entityId, channel)}
 * to the live {@link VfxChannelEffect} and owns the wire-state policy —
 * START/UPDATE open-or-retarget, STOP releases, silence expires.
 * Minecraft-free on purpose (entity resolution arrives through {@link Opener});
 * JUnit drives this directly.
 *
 * <p>Opened channels live in the shared {@link VfxInstanceTable} so the budget
 * cap, ticking and rendering cover them like any other effect; this map is
 * only the address book plus its ages.
 *
 * <p>Late tracking (§7 stage 15) comes free from the open-or-retarget policy:
 * the periodic UPDATE stream that keeps a channel alive also opens it for an
 * observer that missed its START — a tracker entering range mid-laser sees
 * the beam from the next UPDATE tick. One-shot event effects (CLAP,
 * MILK_DRINK) carry no such stream and are simply missed, by design.
 */
final class VfxChannelTable {
	/**
	 * Resolves the wire triple {@code (entityId, channel, target)} to an opened
	 * channel effect, or {@code null} when the channel id is unknown or the
	 * entity is not on the client level.
	 */
	@FunctionalInterface
	interface Opener {
		@Nullable
		VfxChannelEffect open(int entityId, ResourceLocation channel, Vec3 target);
	}

	private record ChannelKey(int entityId, ResourceLocation channel) {
	}

	private static final class Entry {
		final VfxChannelEffect effect;
		int sinceUpdate;

		Entry(VfxChannelEffect effect) {
			this.effect = effect;
		}
	}

	private final VfxInstanceTable instances;
	private final Opener opener;
	private final int timeoutTicks;
	private final Map<ChannelKey, Entry> channels = new LinkedHashMap<>();

	VfxChannelTable(VfxInstanceTable instances, Opener opener, int timeoutTicks) {
		this.instances = instances;
		this.opener = opener;
		this.timeoutTicks = timeoutTicks;
	}

	int size() {
		return channels.size();
	}

	void channel(int entityId, ResourceLocation channel, byte state, Vec3 target) {
		ChannelKey key = new ChannelKey(entityId, channel);
		switch (state) {
			case VfxChannelS2CPayload.START, VfxChannelS2CPayload.UPDATE -> {
				Entry entry = channels.get(key);
				if (entry != null && entry.effect.done()) {
					channels.remove(key);
					entry = null;
				}
				if (entry != null) {
					entry.effect.retarget(target);
					entry.sinceUpdate = 0;
				} else {
					VfxChannelEffect opened = opener.open(entityId, channel, target);
					if (opened != null) {
						channels.put(key, new Entry(opened));
						instances.add(opened);
					}
				}
			}
			case VfxChannelS2CPayload.STOP -> {
				// The entry drops immediately (a re-START opens a fresh channel);
				// the effect stays in the instance table until its release ends.
				Entry entry = channels.remove(key);
				if (entry != null) {
					entry.effect.release();
				}
			}
			default -> {
				// The stream codec rejects other states; ignore defensively.
			}
		}
	}

	void tick() {
		Iterator<Entry> it = channels.values().iterator();
		while (it.hasNext()) {
			Entry entry = it.next();
			if (entry.effect.done()) {
				it.remove();
				continue;
			}
			if (++entry.sinceUpdate > timeoutTicks) {
				entry.effect.release();
				it.remove();
			}
		}
	}

	/** Drops every index entry pointing at {@code effect} (evicted or finished). */
	void forget(VfxEffect effect) {
		channels.values().removeIf(entry -> entry.effect == effect);
	}
}
