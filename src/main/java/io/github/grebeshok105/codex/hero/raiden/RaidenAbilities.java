package io.github.grebeshok105.codex.hero.raiden;

import io.github.grebeshok105.codex.hero.raiden.ability.RaidenEyeOfJudgmentAbility;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenMusouIsshinAbility;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenMusouShinsetsuAbility;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenPlungingStrikeAbility;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenSwordDrawAbility;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenTranscendenceAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Raiden's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class RaidenAbilities {
	public static final ResourceLocation RAIDEN_SWORD_DRAW = RaidenSwordDrawAbility.ID;
	public static final ResourceLocation RAIDEN_EYE_OF_JUDGMENT = RaidenEyeOfJudgmentAbility.ID;
	public static final ResourceLocation RAIDEN_MUSOU_SHINSETSU = RaidenMusouShinsetsuAbility.ID;
	public static final ResourceLocation RAIDEN_MUSOU_ISSHIN = RaidenMusouIsshinAbility.ID;
	public static final ResourceLocation RAIDEN_PLUNGING_STRIKE = RaidenPlungingStrikeAbility.ID;
	public static final ResourceLocation RAIDEN_TRANSCENDENCE = RaidenTranscendenceAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			RAIDEN_SWORD_DRAW,
			RAIDEN_EYE_OF_JUDGMENT,
			RAIDEN_MUSOU_SHINSETSU,
			RAIDEN_MUSOU_ISSHIN,
			RAIDEN_PLUNGING_STRIKE,
			RAIDEN_TRANSCENDENCE
	);

	private RaidenAbilities() {
	}
}
