package io.github.grebeshok105.codex.client.hero.thanos.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.grebeshok105.codex.client.core.hud.AbilityDecoration;
import io.github.grebeshok105.codex.client.hero.thanos.state.ClientThanosState;
import io.github.grebeshok105.codex.client.hud.HudUtil;
import io.github.grebeshok105.codex.hero.thanos.ThanosHero;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityStoneType;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityStones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Per-slot stone badge in the radial menu: the required stone's icon above each
 * stone ability, the gauntlet itself above the snap slot, dimmed with a ✕ while
 * the stone isn't inserted. Moved verbatim out of the radial HUD's thanos branch.
 */
public final class ThanosStoneBadge implements AbilityDecoration {
	@Override
	public void render(GuiGraphics graphics, ResourceLocation aid, int iconCenterX, int iconCenterY, int iconSize) {
		Minecraft mc = Minecraft.getInstance();
		int topY = iconCenterY - iconSize / 2 - 6;

		ItemStack stack;
		int color;
		boolean owned;
		if (ThanosHero.isSnapAbility(aid)) {
			stack = new ItemStack(InfinityStones.gauntletItem());
			color = 0xFFFFD24A;
			owned = ClientThanosState.hasAllStones();
		} else {
			InfinityStoneType type = ThanosHero.getRequiredStoneFor(aid);
			if (type == null) {
				return;
			}
			stack = stoneStackFor(type);
			color = type.getColor();
			owned = ClientThanosState.hasStone(type);
		}
		if (stack == null || stack.isEmpty()) {
			return;
		}
		int badgeSize = 20;
		int iconX = iconCenterX - 8;
		int iconY = topY - badgeSize - 2;
		int bx = iconCenterX - badgeSize / 2;
		int by = iconY - 2;
		HudUtil.roundedRectFill(graphics, bx, by, badgeSize, badgeSize, 0xCC080A14);
		HudUtil.roundedRectBorder(graphics, bx, by, badgeSize, badgeSize, color);
		long now = System.currentTimeMillis();
		float pulse = 0.55f + 0.45f * (float) Math.sin(now / 280.0);
		int glowAlpha = (int) (160 * pulse);
		int glow = ((Math.max(40, glowAlpha) & 0xFF) << 24) | (color & 0x00FFFFFF);
		HudUtil.roundedRectBorder(graphics, bx - 1, by - 1, badgeSize + 2, badgeSize + 2, glow);
		RenderSystem.enableBlend();
		graphics.renderItem(stack, iconX, iconY);
		RenderSystem.disableBlend();
		if (!owned) {
			graphics.fill(bx + 1, by + 1, bx + badgeSize - 1, by + badgeSize - 1, 0xB0000814);
			graphics.drawCenteredString(mc.font, Component.literal("✕"),
					iconCenterX, by + 6, 0xFFE03030);
		}
	}

	private static ItemStack stoneStackFor(InfinityStoneType type) {
		return new ItemStack(InfinityStones.stoneItem(type));
	}
}
