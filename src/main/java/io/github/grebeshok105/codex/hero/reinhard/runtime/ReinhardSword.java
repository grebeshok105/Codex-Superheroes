package io.github.grebeshok105.codex.hero.reinhard.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeaponItem;
import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeapons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Issue/revoke of Reid — Reinhard's bound sword — shared by the ability, the draw ceremony and
 * the hero-clear path. Lives in {@code runtime} because both {@code ability} and {@code runtime}
 * leaf code need it, while {@code item} leaf code already points into {@code runtime} (one-way).
 * The item instance is looked up in the registry so this leaf never imports {@code item/}.
 */
public final class ReinhardSword {
	private static final ResourceLocation ROYAL_ICICLE_ID = ModId.of("royal_icicle");
	private static final ResourceLocation SWORD_DRAW_ID = ModId.of("reinhard_sword_draw");

	public static boolean giveSword(ServerPlayer player) {
		return BoundWeapons.ensureHeld(player, sword());
	}

	public static void removeSword(ServerPlayer player) {
		BoundWeapons.revoke(player, sword());
	}

	public static void forceSheathe(ServerPlayer player) {
		ReinhardState state = player.getAttachedOrCreate(ReinhardState.ATTACHMENT);
		if (!state.swordDrawn()) return;
		player.setAttached(ReinhardState.ATTACHMENT, state.withSwordDrawn(false));
		ReinhardModifiers.REINHARD_DRAW.remove(player);
		removeSword(player);
		ReinhardTimeSlowController.disarmForFirstStrike(player);
		HeroDataStore.update(player, d -> d.withActive(SWORD_DRAW_ID, false));
		player.displayClientMessage(
				Component.translatable("ability.superheroes.reinhard_sword_draw.sheathed"),
				true);
	}

	public static boolean isRoyalIcicle(ItemStack stack) {
		return stack.is(sword());
	}

	private static BoundWeaponItem sword() {
		return (BoundWeaponItem) BuiltInRegistries.ITEM.get(ROYAL_ICICLE_ID);
	}

	private ReinhardSword() {
	}
}
