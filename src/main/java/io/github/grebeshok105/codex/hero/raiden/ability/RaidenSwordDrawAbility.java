package io.github.grebeshok105.codex.hero.raiden.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.hero.raiden.runtime.RaidenState;
import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeaponItem;
import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeapons;
import io.github.grebeshok105.codex.particle.ModParticles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Manifest Yamato — toggle. Призывает фиолетовую Ямато в руку.
 * Без активной Manifest Yamato меч полностью отсутствует у Райден.
 * Деактивация — Ямато исчезает (рассыпается на молнии).
 */
public final class RaidenSwordDrawAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("raiden_sword_draw");
	private static final ResourceLocation YAMATO_ID = ModId.of("musou_no_hitotachi");

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
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		if (!giveSword(player)) {
			player.displayClientMessage(Component.translatable("ability.superheroes.bound_weapon.no_room"), true);
			return false;
		}
		RaidenState state = player.getAttachedOrCreate(RaidenState.ATTACHMENT);
		player.setAttached(RaidenState.ATTACHMENT, state.withSwordDrawn(true));
		ServerLevel level = player.serverLevel();
		level.sendParticles(ModParticles.SWORD_EXPLOSION,
				player.getX(), player.getY() + 1.0, player.getZ(),
				24, 0.4, 0.7, 0.4, 0.18);
		level.sendParticles(ModParticles.JIWALD_EFFECT,
				player.getX(), player.getY() + 1.0, player.getZ(),
				40, 0.5, 0.7, 0.5, 0.25);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9f, 1.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.8f);
		return true;
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		RaidenState state = player.getAttachedOrCreate(RaidenState.ATTACHMENT);
		player.setAttached(RaidenState.ATTACHMENT, state.withSwordDrawn(false));
		removeSword(player);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ModParticles.BLUE_FLAME,
				player.getX(), player.getY() + 1.0, player.getZ(),
				18, 0.4, 0.6, 0.4, 0.04);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.7f, 1.3f);
	}

	public static boolean giveSword(ServerPlayer player) {
		return BoundWeapons.ensureHeld(player, yamato());
	}

	public static void removeSword(ServerPlayer player) {
		BoundWeapons.revoke(player, yamato());
	}

	private static BoundWeaponItem yamato() {
		return (BoundWeaponItem) BuiltInRegistries.ITEM.get(YAMATO_ID);
	}
}
