package com.example.superheroes.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Pandora's «Власть тщеславия» (Vanity Authority): the passive package that is
 * active for as long as her House of Vanity is open.
 *
 * <ul>
 *   <li>Pandora herself gains free flight and effective immunity (she is the
 *       creator of the space — nothing inside can touch her).</li>
 *   <li>Every trapped victim suffers light "maddening" debuffs — nausea,
 *       darkness, weakness and mining fatigue — that wear off the moment they
 *       leave the House.</li>
 * </ul>
 *
 * All of this is reversible: {@link #clear(ServerPlayer)} restores flight when
 * the House closes (and never touches creative/spectator flight).
 */
public final class VanityAuthority {
	/** Resistance amplifier 4 = 100% damage reduction → effective immunity. */
	private static final int IMMUNITY_AMP = 4;
	/** Re-applied every tick with a short duration so it vanishes ~instantly on House close. */
	private static final int SHORT_DURATION = 10;

	private VanityAuthority() {
	}

	/** Per-tick upkeep for Pandora while her House is open: flight + immunity. */
	public static void applyToCaster(ServerPlayer pandora) {
		pandora.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SHORT_DURATION, IMMUNITY_AMP, false, false, false));
		pandora.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, SHORT_DURATION, 0, false, false, false));
		if (!pandora.isCreative() && !pandora.isSpectator()) {
			var abilities = pandora.getAbilities();
			if (!abilities.mayfly) {
				abilities.mayfly = true;
				pandora.onUpdateAbilities();
			}
		}
	}

	/** Restores Pandora's normal flight state when the House closes. */
	public static void clear(ServerPlayer pandora) {
		if (!pandora.isCreative() && !pandora.isSpectator()) {
			var abilities = pandora.getAbilities();
			if (abilities.mayfly) {
				abilities.mayfly = false;
				abilities.flying = false;
				pandora.onUpdateAbilities();
			}
		}
	}

	/** Namespace of the friend's mod — its passive attribute modifiers are stripped by this. */
	private static final String FALBIKS_NAMESPACE = "falbiks_heroes";

	/** Per-second light "maddening" debuffs + power-strip upkeep on a trapped victim. */
	public static void applyToVictim(ServerPlayer victim) {
		// 60-tick windows refreshed each keepalive so they fade right after release.
		victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, false));
		victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
		victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, false));
		victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, false, false, false));
		// Mark them as vanity-stripped: blocks our abilities (AbilityRouter) and, via a
		// soft @Pseudo mixin, the friend's mod abilities/passives too — for ALL heroes,
		// present or future. Refreshed each keepalive so it clears the moment they escape.
		victim.addEffect(new MobEffectInstance(ModEffects.VANITY_STRIPPED, 60, 0, false, false, false));
		stripFalbiksModifiers(victim);
	}

	/**
	 * Removes any attribute modifiers in the {@code falbiks_heroes} namespace from the
	 * victim. The soft mixin freezes the friend's serverTick (so they are not re-applied),
	 * and this clears whatever was already on. Namespace-based, so new heroes are covered
	 * automatically. They are restored naturally once the victim leaves and falbiks ticks again.
	 */
	private static void stripFalbiksModifiers(ServerPlayer victim) {
		var attributes = victim.getAttributes();
		for (var holder : net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.holders().toList()) {
			if (!attributes.hasAttribute(holder)) {
				continue;
			}
			var instance = attributes.getInstance(holder);
			if (instance == null) {
				continue;
			}
			for (var modifier : new java.util.ArrayList<>(instance.getModifiers())) {
				if (FALBIKS_NAMESPACE.equals(modifier.id().getNamespace())) {
					instance.removeModifier(modifier.id());
				}
			}
		}
	}
}
