package io.github.grebeshok105.codex.mixin;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.effect.FlightController;
import io.github.grebeshok105.codex.transform.HeroData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerFlightPoseMixin {
	@Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
	private void superheroes$forceFlightPose(CallbackInfo ci) {
		Player self = (Player) (Object) this;
		HeroData data = self.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		if (FlightController.isFlightActive(data)) {
			if (!self.isFallFlying()) {
				self.startFallFlying();
			}
			if (self.getPose() != Pose.FALL_FLYING) {
				self.setPose(Pose.FALL_FLYING);
			}
			ci.cancel();
		}
	}
}
