package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.transform.HeroDataStore;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.transform.HeroData;
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
			float newEnergy = energy;
			float newMana = mana;
			HeroDataStore.update(player, d -> d.withResources(newEnergy, newMana));
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
