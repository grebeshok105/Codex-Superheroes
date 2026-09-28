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
		entries.add(lookup, ModDamageTypes.LOKI_CHAOS);
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
