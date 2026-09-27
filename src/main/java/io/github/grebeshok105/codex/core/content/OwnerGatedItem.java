package io.github.grebeshok105.codex.core.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * An item that may refuse {@code ItemEntity.playerTouch} pickup. Consulted by
 * {@code KryptoniteShardPickupMixin} so the mixin — and shared code in general — never names a
 * concrete module class; the item owns its own pickup rules.
 */
public interface OwnerGatedItem {
	/** @return {@code true} to veto the pickup (the touch is cancelled). */
	boolean deniesPickup(Player player, ItemStack stack);
}
