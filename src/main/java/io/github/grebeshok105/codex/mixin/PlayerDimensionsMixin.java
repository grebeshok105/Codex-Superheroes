package io.github.grebeshok105.codex.mixin;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.hero.Heroes;
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
		// PUBLIC_HERO is synced to every tracking client (audit B14) — remote players
		// get real hero dimensions instead of the vanilla hitbox.
		net.minecraft.resources.ResourceLocation heroId = self.getAttached(ModAttachments.PUBLIC_HERO);
		if (heroId == null) {
			return;
		}
		Hero hero = Heroes.get(heroId);
		if (hero == null) {
			return;
		}
		EntityDimensions custom = hero.getDimensions(pose);
		if (custom != null) {
			cir.setReturnValue(custom);
		}
	}
}
