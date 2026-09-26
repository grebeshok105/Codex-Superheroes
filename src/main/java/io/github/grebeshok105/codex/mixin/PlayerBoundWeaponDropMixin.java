package io.github.grebeshok105.codex.mixin;

import io.github.grebeshok105.codex.item.bound.BoundWeapons;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bound hero weapons never leave their owner as item entities. */
@Mixin(Player.class)
public abstract class PlayerBoundWeaponDropMixin {
	@Inject(
			method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
			at = @At("HEAD"),
			cancellable = true
	)
	private void superheroes$keepBoundWeapon(ItemStack stack, boolean throwRandomly, boolean includeThrowerName,
			CallbackInfoReturnable<ItemEntity> cir) {
		if (BoundWeapons.interceptDrop((Player) (Object) this, stack)) {
			cir.setReturnValue(null);
		}
	}
}
