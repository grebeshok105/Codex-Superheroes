package com.example.superheroes.effect;

import net.minecraft.resources.ResourceLocation;
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
	 * @param heroId attacker's current hero ID (nullable)
	 * @param target the melee target
	 * @param doomsdayTier 0 if not Doomsday, else the tier level
	 */
	public static void tryApplyBleeding(ResourceLocation heroId, LivingEntity target, int doomsdayTier) {
		if (heroId == null) return;
		String path = heroId.getPath();
		float chance;
		int amplifier;
		switch (path) {
			case "battle_beast" -> { chance = 0.30f; amplifier = 0; }
			case "kratos"       -> { chance = 0.25f; amplifier = 0; }
			case "omniman"      -> { chance = 0.40f; amplifier = 1; }
			case "invincible"   -> { chance = 0.20f; amplifier = 0; }
			case "doomsday"     -> {
				if (doomsdayTier < 3) return;
				chance = 0.50f;
				amplifier = 1;
			}
			default -> { return; }
		}
		if (ThreadLocalRandom.current().nextFloat() < chance) {
			target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 80, amplifier, false, true, true));
		}
	}
}
