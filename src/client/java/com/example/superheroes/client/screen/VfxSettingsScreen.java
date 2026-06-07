package com.example.superheroes.client.screen;

import com.example.superheroes.client.config.SuperheroesClientConfig;
import com.example.superheroes.client.config.SuperheroesClientConfig.VfxMode;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VfxSettingsScreen extends Screen {
	private Button customBtn;
	private Button legacyBtn;

	public VfxSettingsScreen() {
		super(Component.translatable("screen.superheroes.vfx"));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;
		customBtn = Button.builder(Component.translatable("screen.superheroes.vfx.custom"),
				b -> select(VfxMode.CUSTOM)).bounds(cx - 110, cy - 24, 100, 20).build();
		legacyBtn = Button.builder(Component.translatable("screen.superheroes.vfx.legacy"),
				b -> select(VfxMode.LEGACY)).bounds(cx + 10, cy - 24, 100, 20).build();
		this.addRenderableWidget(customBtn);
		this.addRenderableWidget(legacyBtn);
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
				b -> this.onClose()).bounds(cx - 50, cy + 30, 100, 20).build());
		refreshLabels();
	}

	private void select(VfxMode mode) {
		SuperheroesClientConfig.setVfxMode(mode);
		refreshLabels();
	}

	private void refreshLabels() {
		VfxMode current = SuperheroesClientConfig.vfxMode();
		customBtn.setMessage(Component.translatable("screen.superheroes.vfx.custom")
				.append(current == VfxMode.CUSTOM ? Component.literal(" \u25C9") : Component.literal("")));
		legacyBtn.setMessage(Component.translatable("screen.superheroes.vfx.legacy")
				.append(current == VfxMode.LEGACY ? Component.literal(" \u25C9") : Component.literal("")));
	}

	@Override
	public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFFFF);
	}
}
