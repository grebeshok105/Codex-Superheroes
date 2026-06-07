package com.example.superheroes.datagen;

import com.example.superheroes.damage.ModDamageTypes;
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
		entries.add(lookup, ModDamageTypes.GOKU_KAMEHAMEHA);
		entries.add(lookup, ModDamageTypes.GOKU_INSTANT_STRIKE);
		entries.add(lookup, ModDamageTypes.GOKU_SPIRIT_BOMB);
		entries.add(lookup, ModDamageTypes.NARUTO_RASENGAN);
		entries.add(lookup, ModDamageTypes.NARUTO_RASENSHURIKEN);
		entries.add(lookup, ModDamageTypes.NARUTO_BIJUUDAMA);
		entries.add(lookup, ModDamageTypes.KRATOS_BLADE);
		entries.add(lookup, ModDamageTypes.KRATOS_LEVIATHAN);
		entries.add(lookup, ModDamageTypes.LOKI_CHAOS);
		entries.add(lookup, ModDamageTypes.THANOS_SNAP);
		entries.add(lookup, ModDamageTypes.THANOS_COSMIC_SLAM);
		entries.add(lookup, ModDamageTypes.THANOS_MIND_PULSE);
		entries.add(lookup, ModDamageTypes.THANOS_REALITY_TEAR);
		entries.add(lookup, ModDamageTypes.CAP_SHIELD_THROW);
		entries.add(lookup, ModDamageTypes.CAP_SHIELD_SLAM);
		entries.add(lookup, ModDamageTypes.HOMELANDER_EYE_LASER);
		entries.add(lookup, ModDamageTypes.HOMELANDER_HEAT_VISION);
		entries.add(lookup, ModDamageTypes.HOMELANDER_HAND_CLAP);
		entries.add(lookup, ModDamageTypes.HOMELANDER_SONIC_SLAM);
		entries.add(lookup, ModDamageTypes.HOMELANDER_SHOCKWAVE_DIVE);
		entries.add(lookup, ModDamageTypes.HOMELANDER_LIGHTNING_CALL);
		entries.add(lookup, ModDamageTypes.HOMELANDER_ROAR_BOSS);
		entries.add(lookup, ModDamageTypes.HOMELANDER_MELEE);
	}

	@Override
	public String getName() {
		return "Superheroes Damage Types";
	}
}
