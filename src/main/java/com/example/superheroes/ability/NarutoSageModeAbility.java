package com.example.superheroes.ability;

import com.example.superheroes.ModId;
import com.example.superheroes.hero.AttributeModifierSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class NarutoSageModeAbility implements Ability {
	private static final int EXIT_COOLDOWN_TICKS = 200;
	private static final ResourceLocation SAGE_DAMAGE = ModId.of("modifiers/naruto/sage_damage");
	private static final ResourceLocation SAGE_SPEED = ModId.of("modifiers/naruto/sage_speed");
	private static final ResourceLocation SAGE_HP = ModId.of("modifiers/naruto/sage_max_health");
	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ATTACK_DAMAGE, SAGE_DAMAGE, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.add(Attributes.MOVEMENT_SPEED, SAGE_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.add(Attributes.MAX_HEALTH, SAGE_HP, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.build();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.NARUTO_SAGE_MODE;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 35f;
	}

	@Override
	public float costPerTick() {
		return 1.4f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		PASSIVES.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, true, true));
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.4f);
		return true;
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		PASSIVES.remove(player);
		player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EXIT_COOLDOWN_TICKS, 0, true, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EXIT_COOLDOWN_TICKS, 0, true, true, true));
		AbilityCooldowns.setCooldownTicks(player, getId(), EXIT_COOLDOWN_TICKS);
	}

	public static void serverTick(ServerPlayer player) {
		if (player.tickCount % 40 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
		}
	}
}
