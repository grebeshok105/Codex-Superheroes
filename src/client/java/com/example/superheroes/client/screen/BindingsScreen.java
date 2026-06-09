package com.example.superheroes.client.screen;

import com.example.superheroes.client.ClientAbilityFilter;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.network.BindAbilityResourceC2SPayload;
import com.example.superheroes.resource.ResourceKind;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class BindingsScreen extends Screen {
	private static final int BUTTON_WIDTH = 220;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ROW_GAP = 4;

	public BindingsScreen() {
		super(Component.translatable("screen.superheroes.bindings"));
	}

	@Override
	protected void init() {
		List<ResourceLocation> abilities = ClientAbilityFilter.visible();
		int rows = abilities.size();
		int totalHeight = rows * BUTTON_HEIGHT + Math.max(0, rows - 1) * ROW_GAP;
		int yStart = this.height / 2 - totalHeight / 2;
		int xCenter = this.width / 2;

		for (int i = 0; i < rows; i++) {
			ResourceLocation abilityId = abilities.get(i);
			int rowY = yStart + i * (BUTTON_HEIGHT + ROW_GAP);
			ResourceKind current = ClientHeroState.data().binding(abilityId, ResourceKind.ENERGY);
			Button button = Button.builder(
					buildLabel(abilityId, current),
					btn -> {
						boolean manaAvailable = ClientHeroState.manaMax() > 0f;
						ResourceKind next;
						if (!manaAvailable) {
							next = ResourceKind.ENERGY;
						} else {
							next = current == ResourceKind.ENERGY ? ResourceKind.MANA : ResourceKind.ENERGY;
						}
						ClientPlayNetworking.send(new BindAbilityResourceC2SPayload(abilityId, next));
						if (this.minecraft != null) {
							this.minecraft.setScreen(new BindingsScreen());
						}
					}
			).pos(xCenter - BUTTON_WIDTH / 2, rowY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
			this.addRenderableWidget(button);
		}

		Button done = Button.builder(
				Component.translatable("gui.done"),
				btn -> this.onClose()
		).pos(xCenter - 50, yStart + totalHeight + 16).size(100, BUTTON_HEIGHT).build();
		this.addRenderableWidget(done);
	}

	private Component buildLabel(ResourceLocation abilityId, ResourceKind kind) {
		Component name = Component.translatable("ability." + abilityId.getNamespace() + "." + abilityId.getPath());
		Component kindName = Component.translatable("resource.superheroes." + kind.getSerializedName());
		return Component.translatable("screen.superheroes.bindings.row", name, kindName);
	}
}
