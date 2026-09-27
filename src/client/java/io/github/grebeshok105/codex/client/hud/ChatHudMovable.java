package io.github.grebeshok105.codex.client.hud;

import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.client.core.hud.HudBounds;
import io.github.grebeshok105.codex.client.core.hud.MovableHud;
import net.minecraft.client.Minecraft;

/**
 * Editor-side movable for the vanilla chat, which {@code ChatComponentMixin}
 * shifts by the layout offset (plus the hero-panel lift). No render layer of its
 * own — registered with a no-op layer so it exists in {@code HudLayers.movables()}.
 */
public final class ChatHudMovable implements MovableHud {
	public static final ChatHudMovable INSTANCE = new ChatHudMovable();

	private ChatHudMovable() {
	}

	@Override
	public String layoutId() {
		return HudLayoutManager.CHAT;
	}

	@Override
	public HudBounds bounds(int screenWidth, int screenHeight) {
		Minecraft mc = Minecraft.getInstance();
		int autoLift = ClientHeroState.data().hasHero() ? -HudScaler.scale(104) : 0;
		int w = (int) (mc.options.chatWidth().get() * 280) + 40;
		int h = 120;
		int[] off = HudLayoutManager.offset(HudLayoutManager.CHAT);
		return new HudBounds(2 + off[0], screenHeight - 48 - h + autoLift + off[1], w, h);
	}
}
