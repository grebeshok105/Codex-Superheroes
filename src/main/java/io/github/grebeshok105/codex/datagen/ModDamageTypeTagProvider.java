package io.github.grebeshok105.codex.datagen;

import io.github.grebeshok105.codex.bootstrap.ContentModules;
import io.github.grebeshok105.codex.bootstrap.HeroModules;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.damage.ModDamageTypes;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;

import java.util.concurrent.CompletableFuture;

public final class ModDamageTypeTagProvider extends FabricTagProvider<DamageType> {
	public ModDamageTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, net.minecraft.core.registries.Registries.DAMAGE_TYPE, registriesFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// Аудит B20: урон способностей не должен резаться i-frames (invulnerableTime).
		// Исключения — атаки мобов ближнего боя, которые следуют ванильным правилам.
		getOrCreateTagBuilder(DamageTypeTags.BYPASSES_COOLDOWN).add(
				ModDamageTypes.LOKI_CHAOS
		);

		// #superheroes:beam — beam-typed damage for a hero's adaptation check (counts even
		// without a living attacker). Membership comes from module DamageTypeSpec tags below.

		// Hero- and content-owned types join their declared tags via the module specs —
		// no module class names here.
		for (DamageTypeSpec spec : HeroModules.damageTypeSpecs()) {
			for (var tag : spec.tags()) {
				getOrCreateTagBuilder(tag).add(spec.key());
			}
		}
		for (DamageTypeSpec spec : ContentModules.damageTypeSpecs()) {
			for (var tag : spec.tags()) {
				getOrCreateTagBuilder(tag).add(spec.key());
			}
		}
	}
}
