package com.example.superheroes.item;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.effect.RemDemonismController;
import com.example.superheroes.hero.RemHero;
import com.example.superheroes.transform.HeroData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RemMorningStarItem extends SwordItem {
	public RemMorningStarItem(Properties properties) {
		super(Tiers.NETHERITE, properties.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 0, -2.8f)));
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker instanceof ServerPlayer player) {
			HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (RemHero.ID.equals(data.heroId()) && RemDemonismController.isActive(player)) {
				ServerLevel level = player.serverLevel();
				target.invulnerableTime = 0;
				target.hurt(level.damageSources().playerAttack(player), 2.5f);
				target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 10 * 20, 0, false, true, true));
				level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
						target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
						10, 0.25, 0.25, 0.25, 0.08);
				level.playSound(null, target.getX(), target.getY(), target.getZ(),
						SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.9f, 0.75f);
			}
		}
		return super.hurtEnemy(stack, target, attacker);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.LIGHT_PURPLE);
		tooltip.add(TooltipFrame.flavor("item.superheroes.rem_morning_star.lore.line1", ChatFormatting.LIGHT_PURPLE));
		tooltip.add(TooltipFrame.flavor("item.superheroes.rem_morning_star.lore.line2", ChatFormatting.DARK_GRAY));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.LIGHT_PURPLE);
	}
}
