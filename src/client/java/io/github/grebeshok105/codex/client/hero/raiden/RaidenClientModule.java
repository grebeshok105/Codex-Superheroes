package io.github.grebeshok105.codex.client.hero.raiden;

import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.client.ModKeys;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.RaidenHero;
import io.github.grebeshok105.codex.core.net.ActivateAbilityC2SPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public record RaidenClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return RaidenHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.actionKey(new KeyMapping(
				"key.superheroes.raiden_sword_draw",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F,
				ModKeys.CATEGORY), client -> {
			if (client.player != null && ClientHeroState.data().hasHero()) {
				ClientPlayNetworking.send(new ActivateAbilityC2SPayload(AbilityIds.RAIDEN_SWORD_DRAW));
			}
		});
	}
}
