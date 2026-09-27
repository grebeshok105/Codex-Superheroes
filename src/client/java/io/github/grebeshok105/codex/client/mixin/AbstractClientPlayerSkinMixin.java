package io.github.grebeshok105.codex.client.mixin;

import io.github.grebeshok105.codex.client.ClientNanoSuitUpState;
import io.github.grebeshok105.codex.client.core.render.SkinResolver;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerSkinMixin {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void superheroes$forceHeroSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		// Пока идёт нано-сборка костюма, геройский скин не подменяется:
		// броня постепенно проявляется слоем NanoSuitUpLayer поверх игрока.
		if (ClientNanoSuitUpState.suppressHeroSkin(self.getUUID())) {
			return;
		}
		SkinResolver.ResolvedSkin skin = SkinResolver.resolve(self);
		if (skin == null) {
			return;
		}
		PlayerSkin orig = cir.getReturnValue();
		cir.setReturnValue(new PlayerSkin(
				skin.texture() != null ? skin.texture() : DefaultPlayerSkin.getDefaultTexture(),
				null,
				null,
				null,
				Boolean.TRUE.equals(skin.slimModel()) ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE,
				orig != null && orig.secure()
		));
	}
}
