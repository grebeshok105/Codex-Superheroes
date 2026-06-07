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

public final class GokuSuperSaiyanAuraAbility implements Ability {
	private static final ResourceLocation AURA_DAMAGE = ModId.of("modifiers/goku/super_saiyan_damage");
	private static final ResourceLocation AURA_SPEED = ModId.of("modifiers/goku/super_saiyan_speed");
	private static final AttributeModifierSet PASSIVES = AttributeModifierSet.builder()
			.add(Attributes.ATTACK_DAMAGE, AURA_DAMAGE, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.add(Attributes.MOVEMENT_SPEED, AURA_SPEED, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.build();

	@Override
	public ResourceLocation getId() {
		return AbilityIds.GOKU_SUPER_SAIYAN_AURA;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 40f;
	}

	@Override
	public float costPerTick() {
		return 1.8f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		PASSIVES.apply(player);
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, true, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false, true));
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.8f);
		return true;
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		PASSIVES.remove(player);
		player.removeEffect(MobEffects.GLOWING);
	}

	public static void serverTick(ServerPlayer player) {
		if (player.tickCount % 40 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, true, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false, true));
		}
	}
}
