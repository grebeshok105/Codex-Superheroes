package io.github.grebeshok105.codex.client.core.vfx;

/**
 * Creates a one-shot {@link VfxEffect} from a spawn request. Registered per
 * effect id through {@code HeroClientContext.vfx} / {@link VfxRuntime#registerEffect}.
 */
@FunctionalInterface
public interface VfxEffectFactory {
	VfxEffect create(VfxSpawn spawn);
}
