package com.example.superheroes.ability.ironman;

import com.example.superheroes.ModId;
import com.example.superheroes.attachment.ModAttachments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Применяет статы текущего костюма Железного Человека
 * (множители урона/скорости и бонус брони из {@link IronManSuitVariant}).
 * Вызывается при смене костюма и при применении/снятии пассивок героя.
 */
public final class IronManSuitStats {
	private static final ResourceLocation DAMAGE_ID = ModId.of("modifiers/ironman/suit_damage");
	private static final ResourceLocation SPEED_ID = ModId.of("modifiers/ironman/suit_speed");
	private static final ResourceLocation ARMOR_ID = ModId.of("modifiers/ironman/suit_armor");

	private IronManSuitStats() {
	}

	public static void apply(Player player) {
		clear(player);
		int index = player.getAttachedOrCreate(ModAttachments.SUIT_VARIANT);
		IronManSuitVariant variant = IronManSuitVariant.get(index);
		if (variant.damageMultiplier() != 1.0f) {
			addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID,
					variant.damageMultiplier() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}
		if (variant.speedMultiplier() != 1.0f) {
			addModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID,
					variant.speedMultiplier() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}
		if (variant.armorBonus() != 0f) {
			addModifier(player, Attributes.ARMOR, ARMOR_ID,
					variant.armorBonus(), AttributeModifier.Operation.ADD_VALUE);
		}
	}

	public static void clear(Player player) {
		removeModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID);
		removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
		removeModifier(player, Attributes.ARMOR, ARMOR_ID);
	}

	private static void addModifier(Player player, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			ResourceLocation id, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null && instance.getModifier(id) == null) {
			instance.addTransientModifier(new AttributeModifier(id, amount, operation));
		}
	}

	private static void removeModifier(Player player, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			ResourceLocation id) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(id);
		}
	}
}
