package io.github.grebeshok105.codex.ability;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.effect.ReinhardState;
import io.github.grebeshok105.codex.effect.ReinhardSwordDrawCeremonyController;
import io.github.grebeshok105.codex.item.ModItems;
import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeapons;
import io.github.grebeshok105.codex.hero.AbilityScopedModifiers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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
		if (!io.github.grebeshok105.codex.effect.ReinhardSwordDrawGateController.isReady(player)) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.reinhard_sword_draw.no_worthy"),
					true);
			return false;
		}
		io.github.grebeshok105.codex.effect.ReinhardSwordDrawGateController.consumeReady(player);
		ReinhardSwordDrawCeremonyController.startCeremony(player);
		return false;
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		player.setAttached(ModAttachments.REINHARD_STATE, state.withSwordDrawn(false));
		AbilityScopedModifiers.REINHARD_DRAW.remove(player);
		removeSword(player);
		io.github.grebeshok105.codex.effect.ReinhardTimeSlowController.disarmForFirstStrike(player);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.SMOKE,
				player.getX(), player.getY() + 1.0, player.getZ(),
				12, 0.4, 0.6, 0.4, 0.02);
	}

	public static boolean giveSword(ServerPlayer player) {
		return BoundWeapons.ensureHeld(player, ModItems.ROYAL_ICICLE);
	}

	public static void removeSword(ServerPlayer player) {
		BoundWeapons.revoke(player, ModItems.ROYAL_ICICLE);
	}

	public static void forceSheathe(ServerPlayer player) {
		ReinhardState state = player.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
		if (!state.swordDrawn()) return;
		player.setAttached(ModAttachments.REINHARD_STATE, state.withSwordDrawn(false));
		AbilityScopedModifiers.REINHARD_DRAW.remove(player);
		removeSword(player);
		io.github.grebeshok105.codex.effect.ReinhardTimeSlowController.disarmForFirstStrike(player);
		HeroDataStore.update(player, d -> d.withActive(AbilityIds.REINHARD_SWORD_DRAW, false));
		player.displayClientMessage(
				Component.translatable("ability.superheroes.reinhard_sword_draw.sheathed"),
				true);
	}
}
