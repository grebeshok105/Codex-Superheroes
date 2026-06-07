package com.example.superheroes.mixin;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.item.KryptoniteShardItem;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.transform.HeroData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class KryptoniteShardPickupMixin {
	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void superheroes$kryptonitePickupGuard(Player player, CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		ItemStack stack = self.getItem();
		if (stack.isEmpty() || !stack.is(ModItems.KRYPTONITE_SHARD)) return;

		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (data.hasHero() && DoomsdayHero.ID.equals(data.heroId())) {
			ci.cancel();
			return;
		}

		UUID owner = KryptoniteShardItem.getOwner(stack);
		if (owner != null && !owner.equals(player.getUUID())) {
			ci.cancel();
		}
	}
}
