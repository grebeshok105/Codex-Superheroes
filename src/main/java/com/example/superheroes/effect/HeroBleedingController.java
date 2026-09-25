package com.example.superheroes.effect;

import com.example.superheroes.hero.BleedProfile;
import com.example.superheroes.hero.Hero;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies bleeding effect on melee hits for specific heroes.
 * Called from HeroMeleeImpactController.spawnTierOneHitFx().
 */
public final class HeroBleedingController {

	private HeroBleedingController() {
	}

	/**
	 * Bleed profile for a melee hit by {@code heroId}, or {@code null} when the
	 * hero does not apply bleeding. {@code doomsdayTier} is 0 for non-Doomsday
	 * heroes; Doomsday bleeds only from tier 3 up.
	 */
	public static @Nullable BleedProfile bleedFor(ResourceLocation heroId, int doomsdayTier) {
		if (heroId == null) return null;
		String path = heroId.getPath();
		switch (path) {
			case "battle_beast" -> { return new BleedProfile(0.30f, 0); }
			case "kratos"       -> { return new BleedProfile(0.25f, 0); }
			case "omniman"      -> { return new BleedProfile(0.40f, 1); }
			case "invincible"   -> { return new BleedProfile(0.20f, 0); }
			case "doomsday"     -> {
				if (doomsdayTier < 3) return null;
				return new BleedProfile(0.50f, 1);
			}
			default -> { return null; }
		}
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
