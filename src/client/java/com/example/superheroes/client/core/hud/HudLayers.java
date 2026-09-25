package com.example.superheroes.client.core.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Single HudRenderCallback for the mod; layers draw in ascending {@code order}, ties in registration order. */
public final class HudLayers {
	public record Entry(int order, ResourceLocation id, HudLayer layer, @Nullable MovableHud movable) {
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static List<Entry> sorted = List.of();

	private HudLayers() {
	}

	public static void register(int order, ResourceLocation id, HudLayer layer) {
		add(new Entry(order, id, layer, null));
	}

	public static void registerMovable(int order, ResourceLocation id, HudLayer layer, MovableHud movable) {
		add(new Entry(order, id, layer, movable));
	}

	public static void init() {
		HudRenderCallback.EVENT.register((graphics, delta) -> {
			// Spectator mode: hide the entire mod HUD
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.player.isSpectator()) {
				return;
			}
			for (Entry entry : sorted) {
				entry.layer().render(graphics, delta);
			}
		});
	}

	public static List<MovableHud> movables() {
		return sorted.stream().map(Entry::movable).filter(java.util.Objects::nonNull).toList();
	}

	private static void add(Entry entry) {
		ENTRIES.add(entry);
		List<Entry> copy = new ArrayList<>(ENTRIES);
		copy.sort(Comparator.comparingInt(Entry::order));
		sorted = List.copyOf(copy);
	}
}
