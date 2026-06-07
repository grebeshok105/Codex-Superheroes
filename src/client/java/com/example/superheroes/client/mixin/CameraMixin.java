package com.example.superheroes.client.mixin;

import com.example.superheroes.client.fx.ScreenShakeManager;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	public abstract float getYRot();

	@Shadow
	public abstract float getXRot();

	@Inject(method = "setup", at = @At("TAIL"))
	private void superheroes$applyShake(BlockGetter area, Entity focused, boolean thirdPerson,
			boolean inverseView, float tickDelta, CallbackInfo ci) {
		if (ScreenShakeManager.isActive()) {
			float[] off = ScreenShakeManager.sample();
			this.setRotation(this.getYRot() + off[0], this.getXRot() + off[1]);
		}
	}
}
