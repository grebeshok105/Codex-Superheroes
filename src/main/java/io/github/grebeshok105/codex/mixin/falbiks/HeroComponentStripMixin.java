package io.github.grebeshok105.codex.mixin.falbiks;

import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.falbiks.HeroComponentAccess;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cross-mod power strip for Pandora's House of Vanity (task #12).
 *
 * <p>Soft ({@link Pseudo}) mixin into the friend's mod
 * {@code com.falbiks.heroes.core.component.HeroComponent}. Because we hook the
 * single component entry points — {@code tryActivate} (every active ability) and
 * {@code serverTick} (every passive / per-tick effect) — this suppresses the
 * powers of <b>every</b> falbiks hero, including heroes that don't exist yet, as
 * long as the owner carries our {@link ModEffects#VANITY_STRIPPED} marker. Their
 * skin and identity are never touched.
 *
 * <p>Everything is fail-soft: if the friend's mod is absent the mixin simply
 * never applies, and {@code require = 0} means a future signature change won't
 * crash the game — at worst this single hook no-ops. The owner is resolved
 * reflectively via {@link HeroComponentAccess} for the same reason: a
 * {@code @Shadow} field is verified at mixin apply time and would crash on an
 * upstream rename, which {@code require = 0} cannot guard.
 */
@Pseudo
@Mixin(targets = "com.falbiks.heroes.core.component.HeroComponent", remap = false)
public abstract class HeroComponentStripMixin {
	@Inject(method = "tryActivate", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
	private void superheroes$blockActivateWhenVanityStripped(CallbackInfoReturnable<Boolean> cir) {
		Player player = HeroComponentAccess.playerOf(this);
		if (player != null && ModEffects.isVanityStripped(player)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "serverTick", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
	private void superheroes$blockTickWhenVanityStripped(CallbackInfo ci) {
		Player player = HeroComponentAccess.playerOf(this);
		if (player != null && ModEffects.isVanityStripped(player)) {
			ci.cancel();
		}
	}
}
