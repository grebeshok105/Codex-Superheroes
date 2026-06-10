package com.example.superheroes.hero;

import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Hero {
	ResourceLocation getId();

	float getEnergyMax();

	float getEnergyRegenPerTick();

	float getManaMax();

	@Nullable
	EntityDimensions getDimensions(Pose pose);

	List<ResourceLocation> getAbilities();

	ResourceKind getDefaultBinding(ResourceLocation abilityId);

	void applyPassives(Player player);

	void removePassives(Player player);

	boolean cancelsFallDamage(Player player);

	@Nullable
	default ResourceLocation getSkinTexture() {
		return null;
	}

	default void onLanded(ServerPlayer player, LandingImpact impact) {
	}

	default HeroTheme getTheme() {
		return HeroTheme.DEFAULT;
	}

	default HeroHudConfig getHudConfig() {
		return HeroHudConfig.DEFAULT;
	}
}
