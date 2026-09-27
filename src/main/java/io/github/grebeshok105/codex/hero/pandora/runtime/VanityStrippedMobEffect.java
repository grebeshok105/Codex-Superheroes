package io.github.grebeshok105.codex.hero.pandora.runtime;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect: "this player is Vanity-Stripped" — Pandora suppresses their hero powers. */
public class VanityStrippedMobEffect extends MobEffect {
	public static final Holder<MobEffect> VANITY_STRIPPED = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, ModId.of("vanity_stripped"),
			new VanityStrippedMobEffect(MobEffectCategory.HARMFUL, 0xFF2A0A3A)
	);

	protected VanityStrippedMobEffect(MobEffectCategory category, int color) {
		super(category, color);
	}
}
