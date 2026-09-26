package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.hero.BleedProfile;
import io.github.grebeshok105.codex.hero.Hero;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies bleeding effect on melee hits for specific heroes.
 * Called from HeroMeleeImpactController.spawnTierOneHitFx().
 */
public final class HeroBleedingController {

	private HeroBleedingController() {
	}

	/**
	 * @param hero attacker's current hero
	 * @param attacker the melee attacker
	 * @param target the melee target
	 */
	public static void tryApplyBleeding(Hero hero, ServerPlayer attacker, LivingEntity target) {
		BleedProfile bleed = hero.getMeleeBleed(attacker);
		if (bleed == null) return;
		if (ThreadLocalRandom.current().nextFloat() < bleed.chance()) {
			target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 80, bleed.amplifier(), false, true, true));
		}
	}
}
