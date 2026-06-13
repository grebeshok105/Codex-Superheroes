package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientMirrorDimensionState;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.Random;

/**
 * Pandora — House of Vanity text cipher (#11).
 *
 * <p>While the local player is trapped in the House, EVERY piece of text drawn on
 * screen — HUD, chat, action bar, nametags, Iron Man's custom font, the friend's
 * mod HUD, literally anything routed through {@code Font.drawInBatch} — becomes
 * progressively more "encrypted" (vanilla obfuscated glyphs) each second, until it
 * is fully scrambled. The moment the player leaves the House everything reads
 * normally again. Pandora herself is never trapped, so her own UI stays readable.
 *
 * <p>The transform is per-glyph and probabilistic, driven by
 * {@link ClientMirrorDimensionState#cipherStrength()}, so the scramble visibly
 * thickens over time rather than flipping on all at once.
 */
public final class VanityCipher {
	private static final Random RNG = new Random();

	private VanityCipher() {
	}

	private static float strength() {
		return ClientMirrorDimensionState.cipherStrength();
	}

	public static boolean active() {
		return strength() > 0.001f;
	}

	/** Wraps a glyph stream so a {@code strength}-fraction of glyphs render obfuscated. */
	public static FormattedCharSequence cipher(FormattedCharSequence in) {
		float chance = strength();
		if (chance <= 0.001f || in == null) {
			return in;
		}
		return sink -> in.accept((index, style, codePoint) -> {
			Style s = style;
			if (RNG.nextFloat() < chance) {
				s = style.withObfuscated(true);
			}
			return sink.accept(index, s, codePoint);
		});
	}

	/**
	 * Scrambles a raw string by wrapping a {@code strength}-fraction of characters in
	 * vanilla obfuscation codes (§k…§r). Width is preserved (obfuscated glyphs keep size).
	 */
	public static String cipher(String text) {
		float chance = strength();
		if (chance <= 0.001f || text == null || text.isEmpty()) {
			return text;
		}
		StringBuilder out = new StringBuilder(text.length() * 2);
		boolean obf = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			// Never break existing § formatting pairs.
			if (c == '\u00a7' && i + 1 < text.length()) {
				if (obf) {
					out.append('\u00a7').append('r');
					obf = false;
				}
				out.append(c).append(text.charAt(++i));
				continue;
			}
			boolean want = RNG.nextFloat() < chance;
			if (want && !obf) {
				out.append('\u00a7').append('k');
				obf = true;
			} else if (!want && obf) {
				out.append('\u00a7').append('r');
				obf = false;
			}
			out.append(c);
		}
		if (obf) {
			out.append('\u00a7').append('r');
		}
		return out.toString();
	}
}
