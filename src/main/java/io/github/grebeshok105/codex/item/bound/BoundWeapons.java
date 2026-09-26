package io.github.grebeshok105.codex.item.bound;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.item.ModDataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Issues, revokes and validates weapons that hero abilities hand to their owner.
 *
 * <p>Each issue gets a fresh random id recorded in a non-persistent player attachment, so a copy
 * stashed in a container, handed to someone else or left over from a previous session is stale and
 * vanishes when it next ticks in a player inventory. Bound weapons never become item entities.
 */
public final class BoundWeapons {
	private BoundWeapons() {
	}

	/**
	 * Makes sure the owner carries exactly one valid copy, issuing a new one when none is carried.
	 *
	 * @return {@code false} when a new copy was needed but the owner had no free hand or slot
	 */
	public static boolean ensureHeld(ServerPlayer owner, BoundWeaponItem item) {
		if (keepSingleValidCopy(owner, item)) {
			return true;
		}
		long issue = ThreadLocalRandom.current().nextLong();
		ItemStack stack = new ItemStack(item);
		stack.set(ModDataComponents.BOUND_WEAPON, new BoundWeaponToken(owner.getUUID(), issue));
		if (!place(owner, stack)) {
			return false;
		}
		owner.setAttached(ModAttachments.BOUND_WEAPON_ISSUES, issues(owner).with(item, issue));
		return true;
	}

	/** Invalidates the current issue and removes every copy the owner carries. */
	public static void revoke(ServerPlayer owner, BoundWeaponItem item) {
		owner.setAttached(ModAttachments.BOUND_WEAPON_ISSUES, issues(owner).without(item));
		Inventory inventory = owner.getInventory();
		for (NonNullList<ItemStack> compartment : compartments(inventory)) {
			for (int i = 0; i < compartment.size(); i++) {
				if (compartment.get(i).is(item)) {
					compartment.set(i, ItemStack.EMPTY);
				}
			}
		}
		if (owner.containerMenu.getCarried().is(item)) {
			owner.containerMenu.setCarried(ItemStack.EMPTY);
		}
		inventory.setChanged();
	}

	/** True when the stack is the current issue of its item for this holder. */
	public static boolean isValidFor(ItemStack stack, Entity holder) {
		if (!(stack.getItem() instanceof BoundWeaponItem) || !(holder instanceof Player player)) {
			return false;
		}
		BoundWeaponToken token = stack.get(ModDataComponents.BOUND_WEAPON);
		if (token == null || !token.owner().equals(player.getUUID())) {
			return false;
		}
		OptionalLong current = issues(player).issueOf(stack.getItem());
		return current.isPresent() && current.getAsLong() == token.issue();
	}

	static void discardIfInvalid(ItemStack stack, Entity holder) {
		if (!stack.isEmpty() && !isValidFor(stack, holder)) {
			stack.setCount(0);
		}
	}

	/**
	 * Called from the head of {@link Player#drop(ItemStack, boolean, boolean)}. Puts a bound weapon back
	 * into the owner's inventory, or deletes it when it is stale, the player is dying, or there is no room.
	 * Never calls {@code drop} again, so it cannot recurse.
	 *
	 * @return {@code true} when the drop must be cancelled
	 */
	public static boolean interceptDrop(Player player, ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof BoundWeaponItem)) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		boolean keep = player.isAlive() && isValidFor(stack, player);
		if (!keep || !player.getInventory().add(stack)) {
			stack.setCount(0);
		}
		return true;
	}

	/** Leaves at most one valid copy in the owner's hands, inventory and cursor. */
	private static boolean keepSingleValidCopy(ServerPlayer owner, BoundWeaponItem item) {
		boolean kept = false;
		for (NonNullList<ItemStack> compartment : compartments(owner.getInventory())) {
			for (int i = 0; i < compartment.size(); i++) {
				ItemStack stack = compartment.get(i);
				if (!stack.is(item)) {
					continue;
				}
				if (!kept && isValidFor(stack, owner)) {
					kept = true;
				} else {
					compartment.set(i, ItemStack.EMPTY);
				}
			}
		}
		ItemStack carried = owner.containerMenu.getCarried();
		if (carried.is(item)) {
			if (!kept && isValidFor(carried, owner)) {
				kept = true;
			} else {
				owner.containerMenu.setCarried(ItemStack.EMPTY);
			}
		}
		return kept;
	}

	private static boolean place(ServerPlayer owner, ItemStack stack) {
		if (owner.getMainHandItem().isEmpty()) {
			owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
			return true;
		}
		if (owner.getOffhandItem().isEmpty()) {
			owner.setItemInHand(InteractionHand.OFF_HAND, stack);
			return true;
		}
		// Explicit free-slot check: Inventory.add "succeeds" for creative players by deleting the stack.
		Inventory inventory = owner.getInventory();
		int slot = inventory.getFreeSlot();
		if (slot < 0) {
			return false;
		}
		inventory.setItem(slot, stack);
		return true;
	}

	private static BoundWeaponIssues issues(Player player) {
		return player.getAttachedOrElse(ModAttachments.BOUND_WEAPON_ISSUES, BoundWeaponIssues.EMPTY);
	}

	private static List<NonNullList<ItemStack>> compartments(Inventory inventory) {
		return List.of(inventory.items, inventory.armor, inventory.offhand);
	}
}
