package io.github.grebeshok105.codex.hero.sungjinwoo.registry;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.damage.DamageSources;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Sung Jin-Woo's damage types. The key stays byte-identical to the row {@code ModDamageTypes}
 * used to own; the module exposes it through {@code damageTypes()} so datagen and tag
 * providers enumerate it without naming this package.
 */
public final class SungJinwooDamageTypes {
	public static final ResourceKey<DamageType> SHADOW_ATTACK = key("shadow_attack");

	public static final List<DamageTypeSpec> SPECS = List.of(
			DamageTypeSpec.of("shadow_attack", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F)
	);

	private SungJinwooDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ModId.of(name));
	}

	public static DamageSource shadowAttack(ServerLevel level, Entity attacker) {
		return DamageSources.of(level, SHADOW_ATTACK, attacker);
	}
}
