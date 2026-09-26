package com.example.superheroes.client.hero.raiden;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ModKeys;
import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.network.ActivateAbilityC2SPayload;
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
