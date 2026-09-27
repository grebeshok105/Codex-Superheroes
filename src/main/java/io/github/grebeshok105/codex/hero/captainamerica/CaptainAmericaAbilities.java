package io.github.grebeshok105.codex.hero.captainamerica;

import io.github.grebeshok105.codex.hero.captainamerica.ability.CapCounterStanceAbility;
import io.github.grebeshok105.codex.hero.captainamerica.ability.CapShieldDashAbility;
import io.github.grebeshok105.codex.hero.captainamerica.ability.CapShieldSlamAbility;
import io.github.grebeshok105.codex.hero.captainamerica.ability.CapShieldThrowAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Captain America's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class CaptainAmericaAbilities {
	public static final ResourceLocation CAP_SHIELD_THROW = CapShieldThrowAbility.ID;
	public static final ResourceLocation CAP_SHIELD_SLAM = CapShieldSlamAbility.ID;
	public static final ResourceLocation CAP_SHIELD_DASH = CapShieldDashAbility.ID;
	public static final ResourceLocation CAP_COUNTER_STANCE = CapCounterStanceAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			CAP_SHIELD_THROW,
			CAP_SHIELD_SLAM,
			CAP_SHIELD_DASH,
			CAP_COUNTER_STANCE
	);

	private CaptainAmericaAbilities() {
	}
}
