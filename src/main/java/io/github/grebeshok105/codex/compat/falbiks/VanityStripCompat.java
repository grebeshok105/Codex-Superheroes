package io.github.grebeshok105.codex.compat.falbiks;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Checks the {@code superheroes:vanity_stripped} marker effect on a falbiks hero's owner.
 *
 * <p>compat packages must not depend on {@code hero.pandora} (ArchUnit: hero modules are
 * referenced only by themselves and the module list), so the effect holder is resolved by
 * id once here. The lookup is fail-soft: an absent holder means the mod code that applies
 * the marker is gone, which is the same as "never stripped".
 */
public final class VanityStripCompat {
	private static final ResourceLocation MARKER_ID = ModId.of("vanity_stripped");
	@Nullable
	private static volatile Holder<MobEffect> marker;
	private static volatile boolean resolved;

	private VanityStripCompat() {
	}

	public static boolean isStripped(LivingEntity entity) {
		if (entity == null) {
			return false;
		}
		Holder<MobEffect> holder = marker;
		if (holder == null) {
			holder = resolve();
			if (holder == null) {
				return false;
			}
		}
		return entity.hasEffect(holder);
	}

	@Nullable
	private static synchronized Holder<MobEffect> resolve() {
		if (resolved) {
			return marker;
		}
		resolved = true;
		marker = BuiltInRegistries.MOB_EFFECT.getHolder(MARKER_ID).orElse(null);
		return marker;
	}
}
