package io.github.grebeshok105.codex.client.mixin;

import io.github.grebeshok105.codex.client.core.camera.ThirdPersonFraming;
import io.github.grebeshok105.codex.client.fx.ScreenShakeManager;
import net.minecraft.client.Camera;
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

	@Shadow
	protected abstract void setPosition(Vec3 pos);

	/**
	 * Third-person framing (§7 stage 6): inside {@code Camera.setup}'s
	 * detached-only branch, immediately before the zoom collision sweep
	 * reads the base position, shift the base by the registered framing
	 * offset ({@code weight * (bodyCentre - eyePos)} — the
	 * {@code lerp(weight, eyePos, bodyCentre)} from the plan). A {@code null}
	 * offset leaves the vanilla eye position untouched: first-person never
	 * reaches this injection ({@code getMaxZoom} runs only when detached),
	 * non-focused or unframed entities return null, and a player standing
	 * still samples weight 0 → null. {@code getMaxZoom} still traces from
	 * the shifted base, so walls keep blocking the zoom-out. Rotation is
	 * never touched here — only the position base.
	 */
	@Inject(method = "setup",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
	private void superheroes$applyThirdPersonFraming(BlockGetter area, Entity focused,
			boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
		Vec3 offset = ThirdPersonFraming.offset(focused, tickDelta);
		if (offset != null) {
			this.setPosition(this.getPosition().add(offset));
		}
	}

	@Inject(method = "setup", at = @At("TAIL"))
	private void superheroes$applyShake(BlockGetter area, Entity focused, boolean thirdPerson,
			boolean inverseView, float tickDelta, CallbackInfo ci) {
		if (ScreenShakeManager.isActive()) {
			float[] off = ScreenShakeManager.sample();
			this.setRotation(this.getYRot() + off[0], this.getXRot() + off[1]);
		}
	}
}
