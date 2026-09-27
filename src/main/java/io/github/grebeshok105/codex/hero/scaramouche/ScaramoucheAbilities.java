package io.github.grebeshok105.codex.hero.scaramouche;

import io.github.grebeshok105.codex.hero.scaramouche.ability.ScaramoucheElectroSwirlAbility;
import io.github.grebeshok105.codex.hero.scaramouche.ability.ScaramoucheSkyfallBurstAbility;
import io.github.grebeshok105.codex.hero.scaramouche.ability.ScaramoucheWindPrisonAbility;
import io.github.grebeshok105.codex.hero.scaramouche.ability.ScaramoucheWindstepAbility;
import net.minecraft.resources.ResourceLocation;

/**
 * Scaramouche's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 * The shared {@code flight} ability stays in {@code AbilityIds} until wave I3.
 */
public final class ScaramoucheAbilities {
	public static final ResourceLocation SCARAMOUCHE_WINDSTEP = ScaramoucheWindstepAbility.ID;
	public static final ResourceLocation SCARAMOUCHE_ELECTRO_SWIRL = ScaramoucheElectroSwirlAbility.ID;
	public static final ResourceLocation SCARAMOUCHE_WIND_PRISON = ScaramoucheWindPrisonAbility.ID;
	public static final ResourceLocation SCARAMOUCHE_SKYFALL_BURST = ScaramoucheSkyfallBurstAbility.ID;

	private ScaramoucheAbilities() {
	}
}
