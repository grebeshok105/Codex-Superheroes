package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.transform.HeroData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class SuperheroWeaknessEffect extends MobEffect {
	protected SuperheroWeaknessEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (!(entity instanceof ServerPlayer player)) {
			return true;
		}
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return true;
		}
		float energy = data.energy();
		float mana = data.mana();
		boolean dirty = false;
		float drain = 1.0f;
		if (energy > 0f) {
			float used = Math.min(energy, drain);
			energy -= used;
			drain -= used;
			dirty = true;
		}
		if (drain > 0f && mana > 0f) {
			float manaDrain = Math.min(mana, drain * 0.5f);
			mana -= manaDrain;
			dirty = true;
		}
		if (dirty) {
			HeroData updated = data.withResources(energy, mana);
			player.setAttached(ModAttachments.HERO_DATA, updated);
			ModNetworking.syncResources(player, updated);
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
