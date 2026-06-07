package com.example.superheroes.datagen;

import com.example.superheroes.damage.ModDamageTypes;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public final class SuperheroesDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(ModItemModelProvider::new);
		pack.addProvider(ModRecipeProvider::new);
		pack.addProvider(ModDamageTypeProvider::new);
		pack.addProvider(ModDamageTypeTagProvider::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap);
	}
}
