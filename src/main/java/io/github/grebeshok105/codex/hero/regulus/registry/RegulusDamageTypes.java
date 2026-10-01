package io.github.grebeshok105.codex.hero.regulus.registry;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.damage.DamageSources;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Set;

/**
 * Regulus's damage types. Keys stay byte-identical to the rows {@code ModDamageTypes} used to
 * own; the module exposes them through {@code damageTypes()} so datagen and tag providers
 * enumerate them without naming this package.
 */
public final class RegulusDamageTypes {
	public static final ResourceKey<DamageType> COUNTER_STRIKE = key("counter_strike");
	public static final ResourceKey<DamageType> LION_ROAR = key("lion_roar");
	public static final ResourceKey<DamageType> HEART_BACKLASH = key("regulus_heart_backlash");
	public static final ResourceKey<DamageType> LION_HEART_OVERHEAT = key("regulus_lion_heart_overheat");
	public static final ResourceKey<DamageType> BLOOD_PRICE = key("regulus_blood_price");
	public static final ResourceKey<DamageType> DEBRIS = key("regulus_debris");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("counter_strike", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("lion_roar", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("regulus_debris", DamageScaling.NEVER, 0.1F,
					DamageTypeTags.BYPASSES_COOLDOWN),
			DamageTypeSpec.of("regulus_heart_backlash", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_ARMOR, DamageTypeTags.BYPASSES_RESISTANCE,
					DamageTypeTags.BYPASSES_ENCHANTMENTS, DamageTypeTags.BYPASSES_COOLDOWN,
					DamageTypeTags.NO_KNOCKBACK),
			DamageTypeSpec.of("regulus_lion_heart_overheat", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_ARMOR, DamageTypeTags.BYPASSES_RESISTANCE,
					DamageTypeTags.BYPASSES_ENCHANTMENTS, DamageTypeTags.BYPASSES_COOLDOWN,
					DamageTypeTags.NO_KNOCKBACK),
			// The madness tithe: a nameless periodic true damage — no attacker on purpose
			// (nothing to record as lastDamager, nothing to counter).
			DamageTypeSpec.of("regulus_blood_price", DamageScaling.NEVER, 0.0F,
					DamageTypeTags.BYPASSES_ARMOR,
					DamageTypeTags.BYPASSES_RESISTANCE,
					DamageTypeTags.BYPASSES_ENCHANTMENTS,
					DamageTypeTags.BYPASSES_COOLDOWN,
					DamageTypeTags.NO_KNOCKBACK)
	);

	/**
	 * Internal true-cost types — self-inflicted bookkeeping damage that must always
	 * land even through damage-denial windows (the madness reading gate, the
	 * lion-heart void). Matched by key, never instanceof.
	 */
	private static final Set<ResourceKey<DamageType>> INTERNAL = Set.of(
			HEART_BACKLASH, LION_HEART_OVERHEAT, BLOOD_PRICE, COUNTER_STRIKE);

	public static boolean isInternal(DamageSource source) {
		for (ResourceKey<DamageType> key : INTERNAL) {
			if (source.is(key)) {
				return true;
			}
		}
		return false;
	}

	private RegulusDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource counterStrike(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, COUNTER_STRIKE, attacker);
	}

	public static DamageSource debris(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, DEBRIS, attacker);
	}

	/** Heart backlash is a self-cost — no attacker, it always belongs to the owner. */
	public static DamageSource heartBacklash(ServerLevel level) {
		return DamageSources.of(level, HEART_BACKLASH);
	}

	/** Lion-heart overheat burn — a self-cost, like the backlash. */
	public static DamageSource lionHeartOverheat(ServerLevel level) {
		return DamageSources.of(level, LION_HEART_OVERHEAT);
	}

	/** The madness tithe — deliberately attackerless (a ritual price, not a hit). */
	public static DamageSource bloodPrice(ServerLevel level) {
		return new DamageSource(
				level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLOOD_PRICE));
	}
}
