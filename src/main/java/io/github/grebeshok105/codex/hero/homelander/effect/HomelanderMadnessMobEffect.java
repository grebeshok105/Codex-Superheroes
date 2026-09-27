package io.github.grebeshok105.codex.hero.homelander.effect;

import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.model.HeroData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class HomelanderMadnessMobEffect extends MobEffect {
	public HomelanderMadnessMobEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		super.onEffectStarted(entity, amplifier);
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		if (data.hasHero()) {
			Hero hero = Heroes.get(data.heroId());
			if (hero != null) {
				HeroDataStore.update(player, d -> d.withResources(hero.getEnergyMax(), hero.getManaMax()));
			}
		}
		MobEffectInstance current = entity.getEffect(HomelanderEffects.MADNESS);
		int duration = current != null ? current.getDuration() : 300;
		entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 4, true, false, true));
	}
}
