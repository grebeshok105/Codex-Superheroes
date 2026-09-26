package io.github.grebeshok105.codex.hero.scorpion;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.resources.ResourceLocation;

/** Scorpion's ability ids — module-private so shared code never names a hero's abilities. */
public final class ScorpionAbilities {
	public static final ResourceLocation SPEAR = ModId.of("scorpion_spear");
	public static final ResourceLocation HELLFIRE = ModId.of("scorpion_hellfire");
	public static final ResourceLocation FIRE_TELEPORT = ModId.of("scorpion_fire_teleport");
	public static final ResourceLocation HELL_BREATH = ModId.of("scorpion_hell_breath");

	private ScorpionAbilities() {
	}
}
