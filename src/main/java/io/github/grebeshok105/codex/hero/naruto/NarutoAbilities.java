package io.github.grebeshok105.codex.hero.naruto;

import io.github.grebeshok105.codex.hero.naruto.ability.NarutoBijuudamaAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoOodamaRasenganAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoRasenganAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoRasenshurikenAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoSageModeAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoShadowClonesAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Naruto's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class NarutoAbilities {
	public static final ResourceLocation NARUTO_RASENGAN = NarutoRasenganAbility.ID;
	public static final ResourceLocation NARUTO_SHADOW_CLONES = NarutoShadowClonesAbility.ID;
	public static final ResourceLocation NARUTO_RASENSHURIKEN = NarutoRasenshurikenAbility.ID;
	public static final ResourceLocation NARUTO_SAGE_MODE = NarutoSageModeAbility.ID;
	public static final ResourceLocation NARUTO_OODAMA_RASENGAN = NarutoOodamaRasenganAbility.ID;
	public static final ResourceLocation NARUTO_BIJUUDAMA = NarutoBijuudamaAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			NARUTO_RASENGAN,
			NARUTO_SHADOW_CLONES,
			NARUTO_RASENSHURIKEN,
			NARUTO_SAGE_MODE,
			NARUTO_OODAMA_RASENGAN,
			NARUTO_BIJUUDAMA
	);

	private NarutoAbilities() {
	}
}
