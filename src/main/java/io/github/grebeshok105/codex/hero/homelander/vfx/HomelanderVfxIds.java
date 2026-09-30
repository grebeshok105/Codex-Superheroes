package io.github.grebeshok105.codex.hero.homelander.vfx;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.resources.ResourceLocation;

/**
 * Homelander's Visual Core effect/channel ids ({@code superheroes:homelander/<name>}).
 * {@link #LASER} is a channel id ({@code VfxChannelS2CPayload}); the rest are
 * one-shot {@code VfxEventS2CPayload} effect ids. Server code sends them via
 * {@code VfxFx}; {@code client/hero/homelander/fx/HomelanderFx} registers the
 * matching client factories.
 */
public final class HomelanderVfxIds {
	public static final ResourceLocation LANDING = ModId.of("homelander/landing");
	public static final ResourceLocation LASER = ModId.of("homelander/laser");
	public static final ResourceLocation SUN_CHARGE = ModId.of("homelander/sun_charge");
	public static final ResourceLocation SUN_DETONATION = ModId.of("homelander/sun_detonation");
	public static final ResourceLocation MADNESS_CRASH = ModId.of("homelander/madness_crash");
	public static final ResourceLocation IRON_FISTS_ON = ModId.of("homelander/iron_fists_on");
	public static final ResourceLocation IRON_FISTS_OFF = ModId.of("homelander/iron_fists_off");
	public static final ResourceLocation IRON_FISTS_HIT = ModId.of("homelander/iron_fists_hit");
	public static final ResourceLocation CLAP = ModId.of("homelander/clap");
	public static final ResourceLocation ROAR = ModId.of("homelander/roar");
	public static final ResourceLocation MILK_DRINK = ModId.of("homelander/milk_drink");
	public static final ResourceLocation MILK_CANCEL = ModId.of("homelander/milk_cancel");

	private HomelanderVfxIds() {
	}
}
