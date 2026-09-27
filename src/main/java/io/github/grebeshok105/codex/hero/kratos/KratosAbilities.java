package io.github.grebeshok105.codex.hero.kratos;

import io.github.grebeshok105.codex.hero.kratos.ability.KratosBladeStormAbility;
import io.github.grebeshok105.codex.hero.kratos.ability.KratosChainWhirlAbility;
import io.github.grebeshok105.codex.hero.kratos.ability.KratosGodSlayerAbility;
import io.github.grebeshok105.codex.hero.kratos.ability.KratosLeviathanThrowAbility;
import io.github.grebeshok105.codex.hero.kratos.ability.KratosSpartanRageAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Kratos's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class KratosAbilities {
	public static final ResourceLocation KRATOS_SPARTAN_RAGE = KratosSpartanRageAbility.ID;
	public static final ResourceLocation KRATOS_BLADE_STORM = KratosBladeStormAbility.ID;
	public static final ResourceLocation KRATOS_CHAIN_WHIRL = KratosChainWhirlAbility.ID;
	public static final ResourceLocation KRATOS_LEVIATHAN_THROW = KratosLeviathanThrowAbility.ID;
	public static final ResourceLocation KRATOS_GOD_SLAYER = KratosGodSlayerAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			KRATOS_SPARTAN_RAGE,
			KRATOS_BLADE_STORM,
			KRATOS_CHAIN_WHIRL,
			KRATOS_LEVIATHAN_THROW,
			KRATOS_GOD_SLAYER
	);

	private KratosAbilities() {
	}
}
