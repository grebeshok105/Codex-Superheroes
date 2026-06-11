package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.hud.HudLayoutManager;
import com.example.superheroes.client.hud.HudScaler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves the vanilla chat: automatically lifts it above the hero panel when a hero
 * is active, plus applies the user's drag offset from the HUD editor.
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void superheroes$pushChatOffset(GuiGraphics graphics, int tickCount, int mouseX, int mouseY,
			boolean focused, CallbackInfo ci) {
		int[] off = HudLayoutManager.offset(HudLayoutManager.CHAT);
		int autoLift = ClientHeroState.data().hasHero() ? -HudScaler.scale(104) : 0;
		graphics.pose().pushPose();
		graphics.pose().translate(off[0], off[1] + autoLift, 0);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void superheroes$popChatOffset(GuiGraphics graphics, int tickCount, int mouseX, int mouseY,
			boolean focused, CallbackInfo ci) {
		graphics.pose().popPose();
	}
}
