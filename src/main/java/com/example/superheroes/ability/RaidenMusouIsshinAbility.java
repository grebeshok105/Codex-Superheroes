package com.example.superheroes.ability;

import com.example.superheroes.effect.RaidenMusouIsshinController;
import com.example.superheroes.item.MusouNoHitotachiItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Musou Isshin — легендарный удар, разрубивший остров Яширори.
 * После 3-секундной зарядки создаёт электро-разрез длиной 45 блоков.
 * Во время зарядки враги в радиусе 50 блоков замедляются почти до полной остановки.
 * КД 45 секунд, стоимость 800 энергии.
 */
public final class RaidenMusouIsshinAbility implements Ability {
	private static final float COST = 800f;
	private static final int COOLDOWN_TICKS = 45 * 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.RAIDEN_MUSOU_ISSHIN;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return COST;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		if (AbilityCooldowns.isOnCooldown(player, getId())) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.raiden_musou_isshin.cooldown"), true);
			return false;
		}
		if (RaidenMusouIsshinController.isCharging(player)) {
			return false;
		}
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		boolean hasYamato = main.getItem() instanceof MusouNoHitotachiItem || off.getItem() instanceof MusouNoHitotachiItem;
		if (!hasYamato) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.raiden_musou_isshin.no_sword"), true);
			return false;
		}
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		if (!RaidenMusouIsshinController.start(player)) {
			return false;
		}
		player.swing(InteractionHand.MAIN_HAND, true);
		player.displayClientMessage(
				Component.translatable("ability.superheroes.raiden_musou_isshin.charging", "3.0"), true);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
