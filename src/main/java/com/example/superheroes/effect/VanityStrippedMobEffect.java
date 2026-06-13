package com.example.superheroes.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect placed on victims trapped in Pandora's House of Vanity. While it
 * is active, the victim's hero powers — from this mod AND from the friend's mod
 * (falbiks_heroes), present or future — are completely suppressed, while their
 * skin and identity are left untouched. Removed the instant they leave the House.
 */
public final class VanityStrippedMobEffect extends MobEffect {
	public VanityStrippedMobEffect(MobEffectCategory category, int color) {
		super(category, color);
	}
}
