package io.github.grebeshok105.codex.hero.battlebeast;

import io.github.grebeshok105.codex.hero.battlebeast.ability.BattleBeastAxeCleaveAbility;
import io.github.grebeshok105.codex.hero.battlebeast.ability.BattleBeastBloodlustAbility;
import io.github.grebeshok105.codex.hero.battlebeast.ability.BattleBeastPredatorLeapAbility;
import io.github.grebeshok105.codex.hero.battlebeast.ability.BattleBeastWarRoarAbility;
import net.minecraft.resources.ResourceLocation;

/**
 * Battle Beast's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class BattleBeastAbilities {
	public static final ResourceLocation BATTLE_BEAST_PREDATOR_LEAP = BattleBeastPredatorLeapAbility.ID;
	public static final ResourceLocation BATTLE_BEAST_AXE_CLEAVE = BattleBeastAxeCleaveAbility.ID;
	public static final ResourceLocation BATTLE_BEAST_WAR_ROAR = BattleBeastWarRoarAbility.ID;
	public static final ResourceLocation BATTLE_BEAST_BLOODLUST = BattleBeastBloodlustAbility.ID;

	private BattleBeastAbilities() {
	}
}
