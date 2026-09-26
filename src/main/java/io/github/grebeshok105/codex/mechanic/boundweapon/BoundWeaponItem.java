package io.github.grebeshok105.codex.mechanic.boundweapon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

/**
 * A weapon that only exists while a hero ability has issued it. It cannot be dropped, and any copy
 * that is not the owner's current issue disappears as soon as it ticks in a player's inventory.
 */
public abstract class BoundWeaponItem extends SwordItem {
	protected BoundWeaponItem(Tier tier, Properties properties) {
		super(tier, properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
		super.inventoryTick(stack, level, holder, slot, selected);
		if (!level.isClientSide()) {
			BoundWeapons.discardIfInvalid(stack, holder);
		}
	}
}
