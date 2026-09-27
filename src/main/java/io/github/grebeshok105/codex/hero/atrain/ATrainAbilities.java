package io.github.grebeshok105.codex.hero.atrain;

import io.github.grebeshok105.codex.hero.atrain.ability.ATrainAdrenalineRushAbility;
import io.github.grebeshok105.codex.hero.atrain.ability.ATrainHyperspeedAbility;
import io.github.grebeshok105.codex.hero.atrain.ability.ATrainMachDashAbility;
import io.github.grebeshok105.codex.hero.atrain.ability.ATrainSonicBoomAbility;
import net.minecraft.resources.ResourceLocation;

/**
 * A-Train's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class ATrainAbilities {
	public static final ResourceLocation A_TRAIN_MACH_DASH = ATrainMachDashAbility.ID;
	public static final ResourceLocation A_TRAIN_SONIC_BOOM = ATrainSonicBoomAbility.ID;
	public static final ResourceLocation A_TRAIN_HYPERSPEED = ATrainHyperspeedAbility.ID;
	public static final ResourceLocation A_TRAIN_ADRENALINE_RUSH = ATrainAdrenalineRushAbility.ID;

	private ATrainAbilities() {
	}
}
