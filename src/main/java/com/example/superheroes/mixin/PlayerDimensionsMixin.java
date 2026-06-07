package com.example.superheroes.mixin;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.transform.HeroData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDimensionsMixin {
	@Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
	private void superheroes$replaceDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		Player self = (Player) (Object) this;
		HeroData data = self.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) {
			return;
		}
		EntityDimensions custom = hero.getDimensions(pose);
		if (custom != null) {
			cir.setReturnValue(custom);
		}
	}
}
