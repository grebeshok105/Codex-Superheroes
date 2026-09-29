package io.github.grebeshok105.codex.client.core.vfx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * The live-effects list behind {@link VfxRuntime}: insertion-ordered,
 * capacity-budgeted, ticked and rendered as one group (one-shots and channel
 * effects alike). Minecraft-free on purpose — JUnit drives this directly with
 * plain {@link VfxEffect} doubles.
 *
 * <p>Beyond capacity the oldest effect is evicted through {@link VfxEffect#cancel()}.
 * Removal (eviction, {@code done()} sweep) is reported to the removal listener
 * so the channel index can forget the dead effect.
 */
final class VfxInstanceTable {
	private final int maxActive;
	private final Deque<VfxEffect> effects = new ArrayDeque<>();
	private Consumer<VfxEffect> removalListener = effect -> {
	};

	VfxInstanceTable(int maxActive) {
		this.maxActive = maxActive;
	}

	int size() {
		return effects.size();
	}

	void setRemovalListener(Consumer<VfxEffect> listener) {
		removalListener = listener;
	}

	void add(VfxEffect effect) {
		effects.addLast(effect);
		while (effects.size() > maxActive) {
			VfxEffect evicted = effects.pollFirst();
			evicted.cancel();
			removalListener.accept(evicted);
		}
	}

	void tick() {
		if (effects.isEmpty()) {
			return;
		}
		// Snapshot: a tick may spawn or remove effects, and eviction must not
		// tick an effect twice in one pass.
		for (VfxEffect effect : effects.toArray(VfxEffect[]::new)) {
			effect.tick();
		}
		effects.removeIf(effect -> {
			if (effect.done()) {
				removalListener.accept(effect);
				return true;
			}
			return false;
		});
	}

	void render(VfxRenderContext ctx) {
		for (VfxEffect effect : effects) {
			effect.render(ctx);
		}
	}

	void clear() {
		effects.clear();
	}
}
