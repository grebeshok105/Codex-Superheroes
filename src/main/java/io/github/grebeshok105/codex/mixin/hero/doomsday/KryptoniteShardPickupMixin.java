package io.github.grebeshok105.codex.mixin.hero.doomsday;

import io.github.grebeshok105.codex.core.content.OwnerGatedItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class KryptoniteShardPickupMixin {
	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void superheroes$kryptonitePickupGuard(Player player, CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		ItemStack stack = self.getItem();
		if (stack.isEmpty() || !(stack.getItem() instanceof OwnerGatedItem gate)) return;
		if (gate.deniesPickup(player, stack)) {
			ci.cancel();
		}
	}
}
