package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.ReinhardController;
import com.example.superheroes.effect.ReinhardState;
import com.example.superheroes.effect.ReinhardSwordDrawCeremonyController;
import com.example.superheroes.hero.HeroAttributes;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.transform.HeroData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Reid Draw — обнажение меча. Тогглится: пока активен, Рейнхард получает бонусы к статам
 * (атака, скорость, прыжок, attack-speed) и может использовать sword-способности.
 * При активации в руку выдаётся Reid (драконий меч). При деактивации — убирается.
 */
public final class ReinhardSwordDrawAbility implements Ability {
	@Override
	public ResourceLocation getId() {
		return AbilityIds.REINHARD_SWORD_DRAW;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 1.5f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		if (ReinhardSwordDrawCeremonyController.isInCeremony(player)) return false;
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		if (state.swordDrawn()) {
			return true;
		}
		if (ReinhardSwordDrawCeremonyController.isInCeremony(player)) {
			return false;
		}
		if (!com.example.superheroes.effect.ReinhardSwordDrawGateController.isReady(player)) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.reinhard_sword_draw.no_worthy"),
					true);
			return false;
		}
		com.example.superheroes.effect.ReinhardSwordDrawGateController.consumeReady(player);
		ReinhardSwordDrawCeremonyController.startCeremony(player);
		return false;
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		player.setAttached(ModAttachments.REINHARD_STATE, state.withSwordDrawn(false));
		HeroAttributes.REINHARD_DRAW.remove(player);
		removeSword(player);
		com.example.superheroes.effect.ReinhardTimeSlowController.disarmForFirstStrike(player);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.SMOKE,
				player.getX(), player.getY() + 1.0, player.getZ(),
				12, 0.4, 0.6, 0.4, 0.02);
	}

	public static void giveSword(ServerPlayer player) {
		if (player.getMainHandItem().is(ModItems.ROYAL_ICICLE)) return;
		if (player.getOffhandItem().is(ModItems.ROYAL_ICICLE)) return;
		ItemStack stack = new ItemStack(ModItems.ROYAL_ICICLE);
		ItemStack mainHand = player.getMainHandItem();
		if (mainHand.isEmpty()) {
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		} else if (player.getOffhandItem().isEmpty()) {
			player.setItemInHand(InteractionHand.OFF_HAND, stack);
		} else {
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
		}
	}

	public static void removeSword(ServerPlayer player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.is(ModItems.ROYAL_ICICLE)) {
				player.getInventory().setItem(i, ItemStack.EMPTY);
			}
		}
	}

	public static void forceSheathe(ServerPlayer player) {
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		if (!state.swordDrawn()) return;
		player.setAttached(ModAttachments.REINHARD_STATE, state.withSwordDrawn(false));
		HeroAttributes.REINHARD_DRAW.remove(player);
		removeSword(player);
		com.example.superheroes.effect.ReinhardTimeSlowController.disarmForFirstStrike(player);
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (data.activeAbilities().contains(AbilityIds.REINHARD_SWORD_DRAW)) {
			data = data.withActive(AbilityIds.REINHARD_SWORD_DRAW, false);
			player.setAttached(ModAttachments.HERO_DATA, data);
			com.example.superheroes.network.ModNetworking.syncHeroData(player, data);
		}
		player.displayClientMessage(
				Component.translatable("ability.superheroes.reinhard_sword_draw.sheathed"),
				true);
	}
}
