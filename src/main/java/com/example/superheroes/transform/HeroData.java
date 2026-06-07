package com.example.superheroes.transform;

import com.example.superheroes.resource.ResourceKind;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record HeroData(
		Optional<ResourceLocation> currentHero,
		float energy,
		float mana,
		Map<ResourceLocation, ResourceKind> abilityBindings,
		Set<ResourceLocation> activeAbilities
) {
	public static final HeroData EMPTY = new HeroData(Optional.empty(), 0f, 0f, Map.of(), Set.of());

	public static final Codec<HeroData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.optionalFieldOf("current_hero").forGetter(HeroData::currentHero),
			Codec.FLOAT.optionalFieldOf("energy", 0f).forGetter(HeroData::energy),
			Codec.FLOAT.optionalFieldOf("mana", 0f).forGetter(HeroData::mana),
			Codec.unboundedMap(ResourceLocation.CODEC, ResourceKind.CODEC)
					.optionalFieldOf("bindings", Map.of()).forGetter(HeroData::abilityBindings),
			ResourceLocation.CODEC.listOf().xmap(
					l -> (Set<ResourceLocation>) new HashSet<>(l),
					s -> List.copyOf(s)
			).optionalFieldOf("active", Set.of()).forGetter(HeroData::activeAbilities)
	).apply(instance, HeroData::new));

	public HeroData {
		abilityBindings = Map.copyOf(abilityBindings);
		activeAbilities = Set.copyOf(activeAbilities);
	}

	public boolean hasHero() {
		return currentHero.isPresent();
	}

	@Nullable
	public ResourceLocation heroId() {
		return currentHero.orElse(null);
	}

	public boolean isActive(ResourceLocation abilityId) {
		return activeAbilities.contains(abilityId);
	}

	public ResourceKind binding(ResourceLocation abilityId, ResourceKind fallback) {
		return abilityBindings.getOrDefault(abilityId, fallback);
	}

	public HeroData withHero(@Nullable ResourceLocation hero) {
		return new HeroData(Optional.ofNullable(hero), energy, mana, abilityBindings, activeAbilities);
	}

	public HeroData withEnergy(float v) {
		return new HeroData(currentHero, v, mana, abilityBindings, activeAbilities);
	}

	public HeroData withMana(float v) {
		return new HeroData(currentHero, energy, v, abilityBindings, activeAbilities);
	}

	public HeroData withResources(float energyV, float manaV) {
		return new HeroData(currentHero, energyV, manaV, abilityBindings, activeAbilities);
	}

	public HeroData withBinding(ResourceLocation abilityId, ResourceKind kind) {
		Map<ResourceLocation, ResourceKind> map = new HashMap<>(abilityBindings);
		map.put(abilityId, kind);
		return new HeroData(currentHero, energy, mana, map, activeAbilities);
	}

	public HeroData withActive(ResourceLocation abilityId, boolean active) {
		Set<ResourceLocation> set = new HashSet<>(activeAbilities);
		if (active) {
			set.add(abilityId);
		} else {
			set.remove(abilityId);
		}
		return new HeroData(currentHero, energy, mana, abilityBindings, set);
	}

	public HeroData clearActive() {
		return new HeroData(currentHero, energy, mana, abilityBindings, Set.of());
	}
}
