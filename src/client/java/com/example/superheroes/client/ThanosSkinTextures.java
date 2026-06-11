package com.example.superheroes.client;

import com.example.superheroes.ModId;
import com.example.superheroes.item.infinity.InfinityStoneType;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds Thanos skin variants where the gauntlet only shows the infinity
 * stones the player has actually inserted. The base thanos.png has all six
 * stones painted on the right-arm overlay; for missing stones the pixels are
 * painted over with glove leather (or cleared for the overlay-only mind stone).
 * Variants are built lazily per stone bitmask and cached as dynamic textures.
 */
public final class ThanosSkinTextures {
	private static final ResourceLocation FULL_SKIN = ModId.of("textures/entity/hero/thanos.png");
	private static final int FULL_MASK = (1 << InfinityStoneType.values().length) - 1;
	private static final Map<Integer, ResourceLocation> CACHE = new HashMap<>();

	/** stone -> {x, y, replacementABGR (0 = transparent)} pixel edits when the stone is absent. */
	private static final int[][][] STONE_PIXELS = buildStonePixels();

	private ThanosSkinTextures() {
	}

	public static ResourceLocation textureFor(int mask) {
		if (mask >= FULL_MASK || mask < 0) {
			return FULL_SKIN;
		}
		return CACHE.computeIfAbsent(mask, ThanosSkinTextures::build);
	}

	private static ResourceLocation build(int mask) {
		Minecraft mc = Minecraft.getInstance();
		try (InputStream in = mc.getResourceManager().open(FULL_SKIN)) {
			NativeImage img = NativeImage.read(in);
			for (InfinityStoneType type : InfinityStoneType.values()) {
				if ((mask & (1 << type.ordinal())) != 0) {
					continue;
				}
				for (int[] px : STONE_PIXELS[type.ordinal()]) {
					img.setPixelRGBA(px[0], px[1], px[2]);
				}
			}
			ResourceLocation id = ModId.of("thanos_gauntlet_" + mask);
			mc.getTextureManager().register(id, new DynamicTexture(img));
			return id;
		} catch (Exception e) {
			return FULL_SKIN;
		}
	}

	private static int[][][] buildStonePixels() {
		int[][][] out = new int[InfinityStoneType.values().length][][];
		int leatherDark = abgr(0xA1, 0x51, 0x12);
		int leatherWarm = abgr(0xD7, 0x88, 0x38);
		out[InfinityStoneType.POWER.ordinal()] = new int[][]{{40, 46, leatherDark}};
		out[InfinityStoneType.SPACE.ordinal()] = new int[][]{{41, 46, leatherDark}};
		out[InfinityStoneType.REALITY.ordinal()] = new int[][]{{42, 46, leatherDark}};
		out[InfinityStoneType.SOUL.ordinal()] = new int[][]{{43, 46, leatherDark}};
		out[InfinityStoneType.TIME.ordinal()] = new int[][]{{47, 46, leatherWarm}};
		// mind stone sits on the back of the hand (overlay only) -> clear to transparent
		out[InfinityStoneType.MIND.ordinal()] = new int[][]{{41, 44, 0}, {42, 44, 0}};
		return out;
	}

	/** NativeImage uses ABGR packing. */
	private static int abgr(int r, int g, int b) {
		return 0xFF000000 | (b << 16) | (g << 8) | r;
	}
}
