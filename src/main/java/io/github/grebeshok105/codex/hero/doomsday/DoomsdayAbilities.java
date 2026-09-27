package io.github.grebeshok105.codex.hero.doomsday;

import io.github.grebeshok105.codex.hero.doomsday.ability.ChargeTackleAbility;
import io.github.grebeshok105.codex.hero.doomsday.ability.DoomGripAbility;
import io.github.grebeshok105.codex.hero.doomsday.ability.DoomsdayBerserkAbility;
import io.github.grebeshok105.codex.hero.doomsday.ability.DoomsdayBoneSpikeAbility;
import io.github.grebeshok105.codex.hero.doomsday.ability.DoomsdayRoarAbility;
import io.github.grebeshok105.codex.hero.doomsday.ability.DoomsdaySmashAbility;
import net.minecraft.resources.ResourceLocation;

/**
 * Doomsday's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class DoomsdayAbilities {
	public static final ResourceLocation DOOMSDAY_SMASH = DoomsdaySmashAbility.ID;
	public static final ResourceLocation DOOMSDAY_ROAR = DoomsdayRoarAbility.ID;
	public static final ResourceLocation DOOMSDAY_BERSERK = DoomsdayBerserkAbility.ID;
	public static final ResourceLocation DOOMSDAY_BONE_SPIKE = DoomsdayBoneSpikeAbility.ID;
	public static final ResourceLocation DOOMSDAY_CHARGE_TACKLE = ChargeTackleAbility.ID;
	public static final ResourceLocation DOOMSDAY_DOOM_GRIP = DoomGripAbility.ID;

	private DoomsdayAbilities() {
	}
}
