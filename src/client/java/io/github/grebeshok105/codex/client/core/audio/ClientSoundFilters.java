package io.github.grebeshok105.codex.client.core.audio;

import net.minecraft.client.resources.sounds.SoundInstance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Mute predicates consulted by the client sound engine for every played sound. Client modules register
 * via {@code HeroClientContext.soundFilter(...)} during bootstrap; {@code client/core/mixin/SoundEngineMixin}
 * cancels {@code SoundEngine.play} while any predicate returns {@code true}. The engine asks the registry —
 * neither side knows which hero a filter belongs to.
 */
public final class ClientSoundFilters {
	private static final List<Predicate<SoundInstance>> MUTE = new CopyOnWriteArrayList<>();

	private ClientSoundFilters() {
	}

	public static void register(Predicate<SoundInstance> mute) {
		MUTE.add(mute);
	}

	/** {@code true} when at least one registered predicate mutes {@code instance}. */
	public static boolean isMuted(SoundInstance instance) {
		for (Predicate<SoundInstance> mute : MUTE) {
			if (mute.test(instance)) {
				return true;
			}
		}
		return false;
	}
}
