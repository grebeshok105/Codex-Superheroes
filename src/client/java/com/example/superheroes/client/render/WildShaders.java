package com.example.superheroes.client.render;

import com.example.superheroes.ModId;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * Кастомные core-шейдеры Wild HUD (SDF: скруглённые панели, круги/кольца/дуги,
 * сектора радиального меню, круговая маска иконок). Регистрируются через
 * Fabric CoreShaderRegistrationCallback и подхватываются {@link WildRenderer}.
 */
public final class WildShaders {
	private static ShaderInstance rect;
	private static ShaderInstance circle;
	private static ShaderInstance sector;
	private static ShaderInstance iconCircle;

	private WildShaders() {
	}

	public static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context -> {
			context.register(ModId.of("hud_rect"), DefaultVertexFormat.POSITION_TEX, s -> rect = s);
			context.register(ModId.of("hud_circle"), DefaultVertexFormat.POSITION_TEX, s -> circle = s);
			context.register(ModId.of("hud_sector"), DefaultVertexFormat.POSITION_TEX, s -> sector = s);
			context.register(ModId.of("hud_icon_circle"), DefaultVertexFormat.POSITION_TEX, s -> iconCircle = s);
		});
	}

	public static ShaderInstance rect() {
		return rect;
	}

	public static ShaderInstance circle() {
		return circle;
	}

	public static ShaderInstance sector() {
		return sector;
	}

	public static ShaderInstance iconCircle() {
		return iconCircle;
	}

	public static boolean rectReady() {
		return rect != null;
	}

	public static boolean circleReady() {
		return circle != null;
	}

	public static boolean sectorReady() {
		return sector != null;
	}

	public static boolean iconCircleReady() {
		return iconCircle != null;
	}
}
