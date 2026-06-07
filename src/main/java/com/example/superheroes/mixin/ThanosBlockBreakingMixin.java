package com.example.superheroes.mixin;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.transform.HeroData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ThanosBlockBreakingMixin {
	@Inject(method = "hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z",
			at = @At("HEAD"), cancellable = true)
	private void superheroes$thanosCorrectTool(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		Player self = (Player) (Object) this;
		if (isThanos(self)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void superheroes$thanosDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		Player self = (Player) (Object) this;
		if (!isThanos(self)) return;
		float vanilla = cir.getReturnValueF();
		ItemStack diamondPick = new ItemStack(Items.DIAMOND_PICKAXE);
		float diamondSpeed = diamondPick.getDestroySpeed(state);
		float result = Math.max(vanilla, Math.max(diamondSpeed, 8.0f));
		cir.setReturnValue(result);
	}

	private static boolean isThanos(Player player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return ThanosHero.ID.equals(data.heroId());
	}
}
