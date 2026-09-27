package io.github.grebeshok105.codex.hero.kazuha;

import io.github.grebeshok105.codex.hero.kazuha.ability.KazuhaAutumnWhirlwindAbility;
import io.github.grebeshok105.codex.hero.kazuha.ability.KazuhaChihayaburuAbility;
import io.github.grebeshok105.codex.hero.kazuha.ability.KazuhaMapleStormAbility;
import io.github.grebeshok105.codex.hero.kazuha.ability.KazuhaMidareRanzanAbility;
import net.minecraft.resources.ResourceLocation;

/**
 * Kazuha's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class KazuhaAbilities {
	public static final ResourceLocation KAZUHA_CHIHAYABURU = KazuhaChihayaburuAbility.ID;
	public static final ResourceLocation KAZUHA_MIDARE_RANZAN = KazuhaMidareRanzanAbility.ID;
	public static final ResourceLocation KAZUHA_AUTUMN_WHIRLWIND = KazuhaAutumnWhirlwindAbility.ID;
	public static final ResourceLocation KAZUHA_MAPLE_STORM = KazuhaMapleStormAbility.ID;

	private KazuhaAbilities() {
	}
}
