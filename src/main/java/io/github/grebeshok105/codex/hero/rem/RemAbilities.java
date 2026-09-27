package io.github.grebeshok105.codex.hero.rem;

import io.github.grebeshok105.codex.hero.rem.ability.RemHealingMagicAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemHumaIceSpikesAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemIceBurstAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemMaceCraterAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemMorningStarAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemOniKickAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemOniRageAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Rem's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class RemAbilities {
	public static final ResourceLocation REM_HEALING_MAGIC = RemHealingMagicAbility.ID;
	public static final ResourceLocation REM_ICE_BURST = RemIceBurstAbility.ID;
	public static final ResourceLocation REM_ONI_RAGE = RemOniRageAbility.ID;
	public static final ResourceLocation REM_MORNING_STAR = RemMorningStarAbility.ID;
	public static final ResourceLocation REM_MACE_CRATER = RemMaceCraterAbility.ID;
	public static final ResourceLocation REM_ONI_KICK = RemOniKickAbility.ID;
	public static final ResourceLocation REM_HUMA_ICE_SPIKES = RemHumaIceSpikesAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			REM_HEALING_MAGIC,
			REM_ICE_BURST,
			REM_ONI_RAGE,
			REM_MORNING_STAR,
			REM_MACE_CRATER,
			REM_ONI_KICK,
			REM_HUMA_ICE_SPIKES
	);

	private RemAbilities() {
	}
}
