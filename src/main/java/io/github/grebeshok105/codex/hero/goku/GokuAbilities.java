package io.github.grebeshok105.codex.hero.goku;

import io.github.grebeshok105.codex.hero.goku.ability.GokuInstantTransmissionAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuKamehamehaAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuKiChargeAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuSolarFlareAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuSpiritBombAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuSuperSaiyanAuraAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Goku's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class GokuAbilities {
	public static final ResourceLocation GOKU_KAMEHAMEHA = GokuKamehamehaAbility.ID;
	public static final ResourceLocation GOKU_INSTANT_TRANSMISSION = GokuInstantTransmissionAbility.ID;
	public static final ResourceLocation GOKU_KI_CHARGE = GokuKiChargeAbility.ID;
	public static final ResourceLocation GOKU_SOLAR_FLARE = GokuSolarFlareAbility.ID;
	public static final ResourceLocation GOKU_SPIRIT_BOMB = GokuSpiritBombAbility.ID;
	public static final ResourceLocation GOKU_SUPER_SAIYAN_AURA = GokuSuperSaiyanAuraAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			GOKU_KAMEHAMEHA,
			GOKU_INSTANT_TRANSMISSION,
			GOKU_KI_CHARGE,
			GOKU_SOLAR_FLARE,
			GOKU_SPIRIT_BOMB,
			GOKU_SUPER_SAIYAN_AURA
	);

	private GokuAbilities() {
	}
}
