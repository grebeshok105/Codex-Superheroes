package com.example.superheroes.hero;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.effect.ThanosGauntletStateController;
import com.example.superheroes.item.infinity.InfinityStoneType;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class ThanosHero implements Hero {
	public static final ResourceLocation ID = ModId.of("thanos");
	public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/thanos.png");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public float getEnergyMax() {
		return 400f;
	}

	@Override
	public float getEnergyRegenPerTick() {
		return 2.0f;
	}

	@Override
	public float getManaMax() {
		return 0f;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return switch (pose) {
			case CROUCHING -> EntityDimensions.scalable(0.7f, 1.6f).withEyeHeight(1.35f);
			case SWIMMING, FALL_FLYING, SPIN_ATTACK -> EntityDimensions.scalable(0.7f, 0.7f).withEyeHeight(0.45f);
			default -> EntityDimensions.scalable(0.75f, 2.1f).withEyeHeight(1.9f);
		};
	}

	private static final Map<ResourceLocation, InfinityStoneType> ABILITY_STONE = Map.of(
			AbilityIds.THANOS_COSMIC_SLAM, InfinityStoneType.POWER,
			AbilityIds.THANOS_REALITY_TEAR, InfinityStoneType.REALITY,
			AbilityIds.THANOS_MIND_PULSE, InfinityStoneType.MIND,
			AbilityIds.THANOS_TIME_REWIND, InfinityStoneType.TIME,
			AbilityIds.THANOS_SPACE_PORTAL, InfinityStoneType.SPACE,
			AbilityIds.THANOS_SOUL_PULSE, InfinityStoneType.SOUL
	);

	public static InfinityStoneType getRequiredStoneFor(ResourceLocation abilityId) {
		return ABILITY_STONE.get(abilityId);
	}

	public static boolean isSnapAbility(ResourceLocation abilityId) {
		return AbilityIds.THANOS_SNAP.equals(abilityId);
	}

	@Override
	public List<ResourceLocation> getAbilities() {
		return List.of(
				AbilityIds.THANOS_COSMIC_SLAM,
				AbilityIds.THANOS_REALITY_TEAR,
				AbilityIds.THANOS_MIND_PULSE,
				AbilityIds.THANOS_TIME_REWIND,
				AbilityIds.THANOS_SPACE_PORTAL,
				AbilityIds.THANOS_SOUL_PULSE,
				AbilityIds.THANOS_SNAP
		);
	}

	public boolean isAbilityUnlocked(Player player, ResourceLocation abilityId) {
		if (!(player instanceof ServerPlayer sp)) return true;
		EnumSet<InfinityStoneType> stones = ThanosGauntletStateController.getCurrentStones(sp);
		if (AbilityIds.THANOS_SNAP.equals(abilityId)) {
			return stones.size() >= 6;
		}
		InfinityStoneType req = ABILITY_STONE.get(abilityId);
		if (req == null) return true;
		return stones.contains(req);
	}

	public static void notifyMissingStone(ServerPlayer player, ResourceLocation abilityId) {
		if (AbilityIds.THANOS_SNAP.equals(abilityId)) {
			int have = ThanosGauntletStateController.getCurrentStones(player).size();
			player.displayClientMessage(
					Component.translatable("ability.superheroes.thanos_snap.gate_failed", have, 6), true);
			return;
		}
		InfinityStoneType req = ABILITY_STONE.get(abilityId);
		if (req == null) return;
		player.displayClientMessage(
				Component.translatable("ability.superheroes.thanos.gate_failed",
						Component.translatable(req.getStoneNameKey())), true);
	}

	@Override
	public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
		return ResourceKind.ENERGY;
	}

	@Override
	public void applyPassives(Player player) {
		HeroAttributes.THANOS.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false, true));
	}

	@Override
	public void removePassives(Player player) {
		HeroAttributes.THANOS.remove(player);
		HeroAttributes.thanosClearStoneModifiers(player);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.FIRE_RESISTANCE);
	}

	@Override
	public boolean cancelsFallDamage(Player player) {
		return true;
	}

	@Override
	public ResourceLocation getSkinTexture() {
		return SKIN;
	}

	@Override
	public HeroTheme getTheme() {
		return HeroTheme.THANOS;
	}
}
