package io.github.grebeshok105.codex.client.mixin;

import io.github.grebeshok105.codex.client.core.flight.FlightCameraOffsets;
import io.github.grebeshok105.codex.client.fx.ScreenShakeManager;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
	protected abstract void setPosition(Vec3 pos);

	@Shadow
	public abstract Vec3 getPosition();

	@Inject(method = "setup", at = @At("TAIL"))
	private void superheroes$applyShake(BlockGetter area, Entity focused, boolean thirdPerson,
			boolean inverseView, float tickDelta, CallbackInfo ci) {
		// Pandora's death cinematic camera lock is handled by the input mixins (mouse/keyboard
		// are frozen) and her POV swap is server-side via ServerPlayer#setCamera — nothing to do here.
		if (ScreenShakeManager.isActive()) {
			float[] off = ScreenShakeManager.sample();
			this.setRotation(this.getYRot() + off[0], this.getXRot() + off[1]);
		}
	}

	/**
	 * Third-person pivot shift: while a flight presentation owns the focused
	 * entity the feet-pivot tilt has moved its visual center off the vanilla
	 * eye point, so the camera slides by the registered offset and keeps the
	 * tilted body centered. First person ({@code thirdPerson == false}) is
	 * untouched. Hero code plugs in through {@link FlightCameraOffsets} —
	 * mixins never import {@code client.hero.*}.
	 */
	@Inject(method = "setup", at = @At("TAIL"))
	private void superheroes$centerFlightBody(BlockGetter area, Entity focused, boolean thirdPerson,
			boolean inverseView, float tickDelta, CallbackInfo ci) {
		if (!thirdPerson || !(focused instanceof LivingEntity living)) {
			return;
		}
		Vec3 offset = FlightCameraOffsets.of(living, tickDelta);
		if (offset != null) {
			this.setPosition(this.getPosition().add(offset));
		}
	}
}
