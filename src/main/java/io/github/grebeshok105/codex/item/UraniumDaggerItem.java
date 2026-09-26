package io.github.grebeshok105.codex.item;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.resource.ResourceController;
import io.github.grebeshok105.codex.transform.HeroData;
import io.github.grebeshok105.codex.transform.TooltipFrame;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

public class UraniumDaggerItem extends Item {
	private static final ResourceLocation DRAIN_ID = ModId.of("uranium_dagger_drain");

	public UraniumDaggerItem(Properties properties) {
		super(properties.attributes(createAttributes()));
	}

	private static ItemAttributeModifiers createAttributes() {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 4.0, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED,
						new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.0, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.build();
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (target instanceof ServerPlayer victim && victim.level() instanceof ServerLevel level) {
			HeroData data = victim.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (data.hasHero()) {
				victim.hurt(level.damageSources().indirectMagic(attacker, attacker), 10f);
				victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 4, false, true, true));
				victim.addEffect(new MobEffectInstance(ModEffects.SUPERHERO_WEAKNESS, 200, 0, false, true, true));
				ResourceController.tryConsume(victim, DRAIN_ID, 200f);
			}
		}
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipFrame.openDivider(tooltip, ChatFormatting.GREEN);
		tooltip.add(TooltipFrame.flavor("item.superheroes.uranium_dagger.lore.line1", ChatFormatting.GREEN));
		tooltip.add(TooltipFrame.flavor("item.superheroes.uranium_dagger.lore.line2", ChatFormatting.DARK_GRAY));
		tooltip.add(Component.empty());
		tooltip.add(TooltipFrame.bullet("item.superheroes.uranium_dagger.lore.usage", ChatFormatting.GOLD));
		TooltipFrame.closeDivider(tooltip, ChatFormatting.GREEN);
	}
}
