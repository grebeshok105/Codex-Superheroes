package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientPandoraDeathState;
import com.example.superheroes.client.fx.ScreenShakeManager;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
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

	@Shadow
	public abstract Vec3 getPosition();

	@Inject(method = "setup", at = @At("TAIL"))
	private void superheroes$applyShake(BlockGetter area, Entity focused, boolean thirdPerson,
			boolean inverseView, float tickDelta, CallbackInfo ci) {
		// Pandora death cinematic: smoothly steer the killer's view toward Pandora. If they
		// are already facing her this barely moves; if facing away it sweeps around.
		if (ClientPandoraDeathState.active()) {
			float steer = ClientPandoraDeathState.cameraSteer();
			if (steer > 0.001f) {
				Vec3 cam = this.getPosition();
				Vec3 t = ClientPandoraDeathState.target();
				double dx = t.x - cam.x;
				double dy = t.y - cam.y;
				double dz = t.z - cam.z;
				double flat = Math.sqrt(dx * dx + dz * dz);
				if (flat > 1.0E-4) {
					float wantYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
					float wantPitch = (float) (-(Mth.atan2(dy, flat) * (180.0 / Math.PI)));
					float yaw = this.getYRot() + Mth.degreesDifference(this.getYRot(), wantYaw) * steer;
					float pitch = this.getXRot() + (wantPitch - this.getXRot()) * steer;
					this.setRotation(yaw, pitch);
				}
			}
		}
		if (ScreenShakeManager.isActive()) {
			float[] off = ScreenShakeManager.sample();
			this.setRotation(this.getYRot() + off[0], this.getXRot() + off[1]);
		}
	}
}
