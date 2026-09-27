package io.github.grebeshok105.codex.client.core.mixin;

import io.github.grebeshok105.codex.client.core.text.TextObfuscationLayers;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Generic text obfuscation hook driving {@link TextObfuscationLayers}.
 *
 * <p>Hooks the two text entry points of {@link Font#drawInBatch} — the
 * {@code String} and {@code FormattedCharSequence} variants. The {@code Component}
 * overload funnels through the {@code FormattedCharSequence} one, so a single pair
 * of hooks covers <b>all</b> on-screen text: HUD, chat, action bar, nametags,
 * custom fonts, and any other mod's text. Hero modules register layers via
 * {@link TextObfuscationLayers} (Pandora's House-of-Vanity cipher, …).
 */
@Mixin(Font.class)
public abstract class FontObfuscationMixin {

	@ModifyVariable(
			method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private String superheroes$cipherString(String text) {
		return TextObfuscationLayers.apply(text);
	}

	@ModifyVariable(
			method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private FormattedCharSequence superheroes$cipherSequence(FormattedCharSequence text) {
		return TextObfuscationLayers.apply(text);
	}
}
