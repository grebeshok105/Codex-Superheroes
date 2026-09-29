package io.github.grebeshok105.codex.client.core.vfx.debug;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.hud.HudLayers;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxRuntime;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.backend.FallbackVfxBackend;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * In-game VFX/perf overlay toggled by {@code /superheroes vfx hud on|off}: the
 * command sends a {@code superheroes:debug/hud} one-shot event whose scale
 * carries the desired state; the factory registered here is a no-op
 * {@link VfxEffect} that only flips {@link #visible}. While visible the HUD
 * shows live-effect/channel counts, which backend is active, and the
 * {@link VfxPerfProbe} frame stats. Frame durations are sampled inside the
 * per-frame HUD render call — no extra hook needed.
 */
public final class VfxDebugHud {
	static final ResourceLocation HUD_EFFECT = ModId.of("debug/hud");

	/** The effect instance the toggle spawns: present for one tick, renders nothing. */
	private static final VfxEffect TOGGLE = new VfxEffect() {
		@Override
		public void tick() {
		}

		@Override
		public void render(VfxRenderContext ctx) {
		}

		@Override
		public boolean done() {
			return true;
		}
	};

	private static final ResourceLocation HUD_LAYER = ModId.of("vfx_debug");
	private static final int TEXT_COLOR = 0xFFE0E0E0;
	private static final int LABEL_COLOR = 0xFF8FD3FF;

	public static boolean visible;

	private static long lastFrameNanos;

	private VfxDebugHud() {
	}

	public static void init() {
		VfxRuntime.registerEffect(HUD_EFFECT, VfxDebugHud::toggle);
		HudLayers.register(900, HUD_LAYER, VfxDebugHud::render);
	}

	private static VfxEffect toggle(VfxSpawn spawn) {
		visible = spawn.scale() > 0.5f;
		return TOGGLE;
	}

	private static void render(GuiGraphics graphics, DeltaTracker delta) {
		if (!visible) {
			lastFrameNanos = 0L;
			return;
		}
		long now = System.nanoTime();
		if (lastFrameNanos != 0L) {
			VfxPerfProbe.record(now - lastFrameNanos);
		}
		lastFrameNanos = now;

		Minecraft mc = Minecraft.getInstance();
		boolean veil = !(VfxBackends.current() instanceof FallbackVfxBackend);
		String[] lines = {
				"VFX DEBUG",
				String.format("effects: %d  channels: %d", VfxRuntime.activeCount(),
						VfxRuntime.openChannelCount()),
				String.format("veil: %s", veil ? "on" : "off"),
				String.format("avg: %.0f fps  1%% low: %.0f fps  %.1f ms",
						VfxPerfProbe.averageFps(), VfxPerfProbe.onePercentLowFps(),
						VfxPerfProbe.averageFrameMillis()),
		};
		int x = 4;
		int y = 4;
		for (int i = 0; i < lines.length; i++) {
			graphics.drawString(mc.font, lines[i], x, y + i * 10,
					i == 0 ? LABEL_COLOR : TEXT_COLOR, true);
		}
	}
}
