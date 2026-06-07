package com.example.superheroes.mixin;

import com.example.superheroes.item.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class RoyalIcicleNoDropMixin {
	@Inject(
			method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
			at = @At("HEAD"),
			cancellable = true
	)
	private void superheroes$preventRoyalIcicleDrop(ItemStack stack, boolean throwRandomly, boolean includeOwnerName,
													CallbackInfoReturnable<ItemEntity> cir) {
		if (!stack.isEmpty() && stack.is(ModItems.ROYAL_ICICLE)) {
			Player self = (Player) (Object) this;
			if (!self.getInventory().add(stack)) {
				self.getInventory().placeItemBackInInventory(stack);
			}
			cir.setReturnValue(null);
		}
	}
}
