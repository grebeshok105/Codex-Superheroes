package io.github.grebeshok105.codex.hero.reinhard;

import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardAirSlashAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardCounterRiposteAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardDivineAuraAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardJudgmentMarkAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardSpeedJudgmentAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardSwordDrawAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardSwordWaveAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardWishAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Reinhard's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class ReinhardAbilities {
	public static final ResourceLocation REINHARD_SWORD_DRAW = ReinhardSwordDrawAbility.ID;
	public static final ResourceLocation REINHARD_AIR_SLASH = ReinhardAirSlashAbility.ID;
	public static final ResourceLocation REINHARD_JUDGMENT_MARK = ReinhardJudgmentMarkAbility.ID;
	public static final ResourceLocation REINHARD_WISH = ReinhardWishAbility.ID;
	public static final ResourceLocation REINHARD_SWORD_WAVE = ReinhardSwordWaveAbility.ID;
	public static final ResourceLocation REINHARD_COUNTER_RIPOSTE = ReinhardCounterRiposteAbility.ID;
	public static final ResourceLocation REINHARD_DIVINE_AURA = ReinhardDivineAuraAbility.ID;
	public static final ResourceLocation REINHARD_SPEED_JUDGMENT = ReinhardSpeedJudgmentAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			REINHARD_SWORD_DRAW,
			REINHARD_AIR_SLASH,
			REINHARD_SWORD_WAVE,
			REINHARD_COUNTER_RIPOSTE,
			REINHARD_DIVINE_AURA,
			REINHARD_SPEED_JUDGMENT,
			REINHARD_JUDGMENT_MARK,
			REINHARD_WISH
	);

	/**
	 * Moved verbatim from {@code AbilityIds}: the four abilities that require the drawn sword.
	 * No call site consults this today — the only live sword gate is the worthy-opponent check
	 * inside {@link io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardSwordDrawAbility}.
	 */
	public static boolean isReinhardSwordOnly(ResourceLocation id) {
		return REINHARD_AIR_SLASH.equals(id)
				|| REINHARD_SWORD_WAVE.equals(id)
				|| REINHARD_COUNTER_RIPOSTE.equals(id)
				|| REINHARD_DIVINE_AURA.equals(id);
	}

	private ReinhardAbilities() {
	}
}
