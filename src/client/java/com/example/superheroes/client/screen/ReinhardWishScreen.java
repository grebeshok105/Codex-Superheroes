package com.example.superheroes.client.screen;

import com.example.superheroes.network.ReinhardWishConfirmC2SPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ReinhardWishScreen extends Screen {
	private static final int CARD_WIDTH = 132;
	private static final int CARD_HEIGHT = 64;
	private static final int CARD_GAP = 12;

	private final List<String> options;
	private final List<String> adapted;
	private final int wishesUsed;
	private final int wishesMax;

	public ReinhardWishScreen(List<String> options, List<String> adapted, int wishesUsed, int wishesMax) {
		super(Component.translatable("screen.superheroes.reinhard_wish.title"));
		this.options = List.copyOf(options);
		this.adapted = List.copyOf(adapted);
		this.wishesUsed = wishesUsed;
		this.wishesMax = wishesMax;
	}

	@Override
	protected void init() {
		int n = Math.min(5, options.size());
		int totalWidth = n * CARD_WIDTH + Math.max(0, n - 1) * CARD_GAP;
		int xStart = (this.width - totalWidth) / 2;
		int yCenter = this.height / 2 - CARD_HEIGHT / 2;

		for (int i = 0; i < n; i++) {
			final String typeId = options.get(i);
			int x = xStart + i * (CARD_WIDTH + CARD_GAP);
			boolean alreadyAdapted = adapted.contains(typeId);
			Button button = Button.builder(
					buildLabel(typeId, alreadyAdapted),
					btn -> {
						if (!alreadyAdapted) {
							ClientPlayNetworking.send(new ReinhardWishConfirmC2SPayload(typeId));
						}
						if (this.minecraft != null) this.minecraft.setScreen(null);
					}
			).bounds(x, yCenter, CARD_WIDTH, CARD_HEIGHT).build();
			button.active = !alreadyAdapted;
			this.addRenderableWidget(button);
		}

		Button cancel = Button.builder(
				Component.translatable("screen.superheroes.reinhard_wish.cancel"),
				btn -> {
					if (this.minecraft != null) this.minecraft.setScreen(null);
				}
		).bounds(this.width / 2 - 60, yCenter + CARD_HEIGHT + 24, 120, 20).build();
		this.addRenderableWidget(cancel);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);

		int titleY = this.height / 2 - CARD_HEIGHT / 2 - 28;
		Component title = Component.translatable("screen.superheroes.reinhard_wish.title");
		graphics.drawCenteredString(this.font, title, this.width / 2, titleY, 0xFFFF6464);

		Component sub = Component.translatable("screen.superheroes.reinhard_wish.subtitle", wishesUsed, wishesMax);
		graphics.drawCenteredString(this.font, sub, this.width / 2, titleY + 12, 0xFFE0E0E0);

		int n = Math.min(5, options.size());
		int totalWidth = n * CARD_WIDTH + Math.max(0, n - 1) * CARD_GAP;
		int xStart = (this.width - totalWidth) / 2;
		int yCenter = this.height / 2 - CARD_HEIGHT / 2;

		RenderSystem.enableBlend();
		for (int i = 0; i < n; i++) {
			String typeId = options.get(i);
			int x = xStart + i * (CARD_WIDTH + CARD_GAP);
			ItemStack icon = iconForDamageType(typeId);
			int iconX = x + (CARD_WIDTH - 16) / 2;
			int iconY = yCenter + 8;
			graphics.renderItem(icon, iconX, iconY);
			graphics.renderItemDecorations(this.font, icon, iconX, iconY);
		}
		RenderSystem.disableBlend();
	}

	private static Component buildLabel(String typeId, boolean alreadyAdapted) {
		String label = labelFor(typeId);
		if (alreadyAdapted) {
			return Component.translatable("screen.superheroes.reinhard_wish.already_adapted", label);
		}
		return Component.literal("\n\n" + label);
	}

	private static String labelFor(String typeId) {
		String s = typeId == null ? "" : typeId;
		if (s.startsWith("minecraft:")) s = s.substring("minecraft:".length());
		String[] parts = s.split("[:_/]");
		StringBuilder sb = new StringBuilder();
		for (String p : parts) {
			if (p.isEmpty()) continue;
			if (sb.length() > 0) sb.append(' ');
			sb.append(Character.toUpperCase(p.charAt(0)));
			if (p.length() > 1) sb.append(p.substring(1));
		}
		return sb.toString();
	}

	private static ItemStack iconForDamageType(String typeId) {
		String key = typeId == null ? "" : typeId;
		if (key.startsWith("minecraft:")) key = key.substring("minecraft:".length());
		return new ItemStack(switch (key) {
			case "arrow", "spit", "trident", "thrown" -> Items.ARROW;
			case "in_fire", "on_fire", "lava", "hot_floor", "campfire" -> Items.FLINT_AND_STEEL;
			case "fireball", "unattributed_fireball" -> Items.FIRE_CHARGE;
			case "explosion", "player_explosion" -> Items.TNT;
			case "fall", "stalagmite", "fly_into_wall" -> Items.LEATHER_BOOTS;
			case "freeze" -> Items.PACKED_ICE;
			case "magic", "indirect_magic", "thorns" -> Items.POTION;
			case "lightning_bolt" -> Items.LIGHTNING_ROD;
			case "wither", "wither_skull" -> Items.WITHER_SKELETON_SKULL;
			case "drown" -> Items.WATER_BUCKET;
			case "starve" -> Items.ROTTEN_FLESH;
			case "cactus", "sweet_berry_bush" -> Items.CACTUS;
			case "dragon_breath" -> Items.DRAGON_BREATH;
			case "sonic_boom" -> Items.SCULK_SHRIEKER;
			case "mob_attack", "mob_attack_no_aggro", "mob_projectile" -> Items.IRON_SWORD;
			case "player_attack" -> Items.NETHERITE_SWORD;
			case "fireworks" -> Items.FIREWORK_ROCKET;
			default -> Items.BARRIER;
		});
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	public static ReinhardWishScreen of(List<String> options, List<String> adapted, int used, int max) {
		List<String> opts = new ArrayList<>(options.size());
		for (String s : options) {
			if (s != null && !s.isEmpty()) opts.add(s);
		}
		return new ReinhardWishScreen(opts, adapted, used, max);
	}
}
