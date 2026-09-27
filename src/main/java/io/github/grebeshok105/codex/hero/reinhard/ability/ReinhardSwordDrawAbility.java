package io.github.grebeshok105.codex.hero.reinhard.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardModifiers;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardState;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardSword;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardSwordDrawCeremonyController;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardSwordDrawGateController;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardTimeSlowController;
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
	public static final ResourceLocation ID = ModId.of("reinhard_sword_draw");

	@Override
	public ResourceLocation getId() {
		return ID;
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
		ReinhardState state = player.getAttachedOrCreate(ReinhardState.ATTACHMENT);
		if (state.swordDrawn()) {
			return true;
		}
		if (ReinhardSwordDrawCeremonyController.isInCeremony(player)) {
			return false;
		}
		if (!ReinhardSwordDrawGateController.isReady(player)) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.reinhard_sword_draw.no_worthy"),
					true);
			return false;
		}
		ReinhardSwordDrawGateController.consumeReady(player);
		ReinhardSwordDrawCeremonyController.startCeremony(player);
		return false;
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		ReinhardState state = player.getAttachedOrCreate(ReinhardState.ATTACHMENT);
		player.setAttached(ReinhardState.ATTACHMENT, state.withSwordDrawn(false));
		ReinhardModifiers.REINHARD_DRAW.remove(player);
		removeSword(player);
		ReinhardTimeSlowController.disarmForFirstStrike(player);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.SMOKE,
				player.getX(), player.getY() + 1.0, player.getZ(),
				12, 0.4, 0.6, 0.4, 0.02);
	}

	public static boolean giveSword(ServerPlayer player) {
		return ReinhardSword.giveSword(player);
	}

	public static void removeSword(ServerPlayer player) {
		ReinhardSword.removeSword(player);
	}

	public static void forceSheathe(ServerPlayer player) {
		ReinhardState state = player.getAttachedOrCreate(ReinhardState.ATTACHMENT);
		if (!state.swordDrawn()) return;
		player.setAttached(ReinhardState.ATTACHMENT, state.withSwordDrawn(false));
		ReinhardModifiers.REINHARD_DRAW.remove(player);
		removeSword(player);
		ReinhardTimeSlowController.disarmForFirstStrike(player);
		HeroDataStore.update(player, d -> d.withActive(ID, false));
		player.displayClientMessage(
				Component.translatable("ability.superheroes.reinhard_sword_draw.sheathed"),
				true);
	}
}
