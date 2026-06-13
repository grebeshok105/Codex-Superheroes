package com.example.superheroes.client.mixin;

import com.example.superheroes.client.hud.VanityCipher;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Pandora — House of Vanity universal text cipher (#11).
 *
 * <p>Hooks the two text entry points of {@link Font#drawInBatch} — the
 * {@code String} and {@code FormattedCharSequence} variants. The {@code Component}
 * overload funnels through the {@code FormattedCharSequence} one, so a single pair
 * of hooks covers <b>all</b> on-screen text: HUD, chat, action bar, nametags,
 * custom fonts (Iron Man), and any other mod's text. The transform only fires while
 * the local player is trapped in the House and scales up each second.
 */
@Mixin(Font.class)
public abstract class FontVanityCipherMixin {

	@ModifyVariable(
			method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private String superheroes$cipherString(String text) {
		return VanityCipher.active() ? VanityCipher.cipher(text) : text;
	}

	@ModifyVariable(
			method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private FormattedCharSequence superheroes$cipherSequence(FormattedCharSequence text) {
		return VanityCipher.active() ? VanityCipher.cipher(text) : text;
	}
}
