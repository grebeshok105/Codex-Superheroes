package io.github.grebeshok105.codex.datagen;

import io.github.grebeshok105.codex.bootstrap.ContentModules;
import io.github.grebeshok105.codex.bootstrap.HeroModules;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.damage.ModDamageTypes;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;

import java.util.concurrent.CompletableFuture;

public final class ModDamageTypeProvider extends FabricDynamicRegistryProvider {
	public ModDamageTypeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		var lookup = registries.lookupOrThrow(Registries.DAMAGE_TYPE);
		entries.add(lookup, ModDamageTypes.EYE_LASER);
		entries.add(lookup, ModDamageTypes.REPULSOR);
		entries.add(lookup, ModDamageTypes.UNIBEAM);
		entries.add(lookup, ModDamageTypes.COUNTER_STRIKE);
		entries.add(lookup, ModDamageTypes.LION_ROAR);
		entries.add(lookup, ModDamageTypes.DOOMSDAY_SMASH);
		entries.add(lookup, ModDamageTypes.DOOMSDAY_ROAR);
		entries.add(lookup, ModDamageTypes.DOOMSDAY_BONE_SPIKE);
		entries.add(lookup, ModDamageTypes.DOOMSDAY_CHARGE_TACKLE);
		entries.add(lookup, ModDamageTypes.DOOMSDAY_DOOM_GRIP);
		entries.add(lookup, ModDamageTypes.SHADOW_ATTACK);
		entries.add(lookup, ModDamageTypes.LOKI_CHAOS);
		entries.add(lookup, ModDamageTypes.THANOS_SNAP);
		entries.add(lookup, ModDamageTypes.THANOS_COSMIC_SLAM);
		entries.add(lookup, ModDamageTypes.THANOS_MIND_PULSE);
		entries.add(lookup, ModDamageTypes.THANOS_REALITY_TEAR);
		entries.add(lookup, ModDamageTypes.CAP_SHIELD_THROW);
		entries.add(lookup, ModDamageTypes.CAP_SHIELD_SLAM);
		entries.add(lookup, ModDamageTypes.SPACE_CRUSH);
		for (DamageTypeSpec spec : HeroModules.damageTypeSpecs()) {
			entries.add(lookup, spec.key());
		}
		for (DamageTypeSpec spec : ContentModules.damageTypeSpecs()) {
			entries.add(lookup, spec.key());
		}
	}

	@Override
	public String getName() {
		return "Superheroes Damage Types";
	}
}
