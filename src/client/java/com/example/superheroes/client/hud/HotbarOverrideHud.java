package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientHeroState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class HotbarOverrideHud {
	private static final int BASE_SLOT_SIZE = 18;
	private static final int BASE_GAP = 1;
	private static final int BASE_MARGIN = 8;

	private HotbarOverrideHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !ClientHeroState.data().hasHero()) {
			return;
		}
		if (mc.options.hideGui) {
			return;
		}

		int screenH = HudScaler.screenHeight();
		int margin = HudScaler.scale(BASE_MARGIN);
		int slotSize = HudScaler.scale(BASE_SLOT_SIZE);
		int gap = HudScaler.scale(BASE_GAP);
		int panelH = HudScaler.scale(130);

		int[] off = HudLayoutManager.offset(HudLayoutManager.HOTBAR);
		int hotbarY = screenH - panelH - margin - slotSize - HudScaler.scale(4) + off[1];
		int hotbarX = margin + off[0];

		Inventory inv = mc.player.getInventory();
		int selected = inv.selected;

		for (int i = 0; i < 9; i++) {
			int sx = hotbarX + i * (slotSize + gap);
			boolean sel = i == selected;

			// Slot bg
			int bg = sel ? 0xCC333355 : 0xAA111122;
			HudUtil.roundedRectFill(graphics, sx, hotbarY, slotSize, slotSize, bg);
			if (sel) {
				HudUtil.roundedRectBorder(graphics, sx, hotbarY, slotSize, slotSize, 0xFFCCCCFF);
			}

			// Item rendering
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty()) {
				int itemX = sx + (slotSize - 16) / 2;
				int itemY = hotbarY + (slotSize - 16) / 2;
				graphics.renderItem(stack, itemX, itemY);
				graphics.renderItemDecorations(mc.font, stack, itemX, itemY);
			}
		}
	}

	public static boolean shouldSuppressVanillaHotbar() {
		return ClientHeroState.data().hasHero();
	}
}
