package io.github.grebeshok105.codex.hero.homelander;

import io.github.grebeshok105.codex.hero.homelander.ability.EyeLasersAbility;
import io.github.grebeshok105.codex.hero.homelander.ability.HandClapAbility;
import io.github.grebeshok105.codex.hero.homelander.ability.IronFistsAbility;
import io.github.grebeshok105.codex.hero.homelander.ability.StunningRoarAbility;
import io.github.grebeshok105.codex.hero.homelander.ability.XRayAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Homelander's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class HomelanderAbilityIds {
	public static final ResourceLocation EYE_LASERS = EyeLasersAbility.ID;
	public static final ResourceLocation X_RAY = XRayAbility.ID;
	public static final ResourceLocation IRON_FISTS = IronFistsAbility.ID;
	public static final ResourceLocation HAND_CLAP = HandClapAbility.ID;
	public static final ResourceLocation STUNNING_ROAR = StunningRoarAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			EYE_LASERS,
			X_RAY,
			IRON_FISTS,
			HAND_CLAP,
			STUNNING_ROAR
	);

	private HomelanderAbilityIds() {
	}
}
