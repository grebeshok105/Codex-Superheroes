package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.transform.HeroData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class MadnessMobEffect extends MobEffect {
	protected MadnessMobEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		super.onEffectStarted(entity, amplifier);
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (data.hasHero()) {
			Hero hero = Heroes.get(data.heroId());
			if (hero != null) {
				HeroData refilled = data.withResources(hero.getEnergyMax(), hero.getManaMax());
				player.setAttached(ModAttachments.HERO_DATA, refilled);
				ModNetworking.syncResources(player, refilled);
			}
		}
		MobEffectInstance current = entity.getEffect(ModEffects.MADNESS);
		int duration = current != null ? current.getDuration() : 300;
		entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 4, true, false, true));
	}
}
