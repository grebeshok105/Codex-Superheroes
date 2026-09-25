package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.hud.HudLayoutManager;
import com.example.superheroes.client.hud.HudScaler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves the vanilla chat: automatically lifts it above the hero panel when a hero
 * is active, plus applies the user's drag offset from the HUD editor.
 *
 * <p>Hit-testing (audit B15): the pose shift moves where chat is drawn but the
 * screen→chat conversions in {@code screenToChatX}/{@code screenToChatY} kept the
 * unshifted mapping, so link clicks and message-tag hovers landed on the wrong
 * line (or nothing). Unshifting the incoming screen coordinates by the same
 * offset at those two private helpers fixes every caller — link clicks, tag
 * hovers, queued-message clicks and the render-time hovered-line highlight —
 * with a single source of truth for the shift.
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void superheroes$pushChatOffset(GuiGraphics graphics, int tickCount, int mouseX, int mouseY,
			boolean focused, CallbackInfo ci) {
		int[] off = superheroes$chatShift();
		graphics.pose().pushPose();
		graphics.pose().translate(off[0], off[1], 0);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void superheroes$popChatOffset(GuiGraphics graphics, int tickCount, int mouseX, int mouseY,
			boolean focused, CallbackInfo ci) {
		graphics.pose().popPose();
	}

	@ModifyVariable(method = "screenToChatX", at = @At("HEAD"), argsOnly = true)
	private double superheroes$unshiftScreenX(double x) {
		return x - superheroes$chatShift()[0];
	}

	@ModifyVariable(method = "screenToChatY", at = @At("HEAD"), argsOnly = true)
	private double superheroes$unshiftScreenY(double y) {
		return y - superheroes$chatShift()[1];
	}

	private static int[] superheroes$chatShift() {
		int[] off = HudLayoutManager.offset(HudLayoutManager.CHAT);
		int autoLift = ClientHeroState.data().hasHero() ? -HudScaler.scale(104) : 0;
		return new int[] { off[0], off[1] + autoLift };
	}
}
