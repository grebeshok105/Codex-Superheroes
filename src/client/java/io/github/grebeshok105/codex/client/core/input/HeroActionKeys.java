package io.github.grebeshok105.codex.client.core.input;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Hero-owned action keys: each entry pairs a {@link KeyMapping} with a hero id; a single END_CLIENT_TICK
 * drains every mapping and fires {@code onPress} only while the local player's {@code PUBLIC_HERO}
 * attachment is that hero. Clicks are always consumed so presses from another hero cannot pile up.
 */
public final class HeroActionKeys {
	private record Entry(KeyMapping mapping, ResourceLocation heroId, Consumer<Minecraft> onPress) {
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static boolean tickInstalled;

	private HeroActionKeys() {
	}

	public static KeyMapping register(ResourceLocation heroId, KeyMapping mapping, Consumer<Minecraft> onPress) {
		KeyMapping registered = KeyBindingHelper.registerKeyBinding(mapping);
		ENTRIES.add(new Entry(registered, heroId, onPress));
		installTick();
		return registered;
	}

	private static void installTick() {
		if (tickInstalled) {
			return;
		}
		tickInstalled = true;
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			LocalPlayer player = client.player;
			ResourceLocation currentHero = player != null ? player.getAttached(ModAttachments.PUBLIC_HERO) : null;
			for (Entry entry : ENTRIES) {
				while (entry.mapping().consumeClick()) {
					if (entry.heroId().equals(currentHero)) {
						entry.onPress().accept(client);
					}
				}
			}
		});
	}
}
