package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientShadowArmyState;
import com.example.superheroes.hero.SungJinwooHero;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
	@ModifyVariable(
			method = "renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V",
			at = @At("STORE"),
			ordinal = 0
	)
	private ResourceLocation superheroes$useHeroHandTexture(ResourceLocation original, PoseStack poseStack, MultiBufferSource multiBufferSource, int light,
			AbstractClientPlayer player, ModelPart arm, ModelPart sleeve) {
		ResourceLocation texture = superheroes$getHeroTexture(player);
		return texture == null ? original : texture;
	}

	@Unique
	private static ResourceLocation superheroes$getHeroTexture(AbstractClientPlayer player) {
		if (player != Minecraft.getInstance().player) {
			return null;
		}
		// Во время нано-сборки рука от первого лица остаётся «голой» — броня ещё материализуется.
		if (com.example.superheroes.client.ClientNanoSuitUpState.suppressHeroSkin(player.getUUID())) {
			return null;
		}
		if (!ClientHeroState.data().hasHero()) {
			return null;
		}
		Hero hero = Heroes.get(ClientHeroState.data().heroId());
		if (hero == null) return null;
		if (SungJinwooHero.ID.equals(hero.getId()) && ClientShadowArmyState.isPhase2(player.getUUID())) {
			return SungJinwooHero.SKIN_PHASE_2;
		}
		// Iron Man: рука от первого лица обязана соответствовать выбранному варианту костюма
		if (com.example.superheroes.hero.IronManHero.ID.equals(hero.getId())) {
			int variant = com.example.superheroes.client.ClientSuitVariantState.variantFor(player.getUUID());
			return com.example.superheroes.ability.ironman.IronManSuitVariant.get(variant).texture();
		}
		// Thanos: перчатка с собранными камнями и от первого лица
		if (com.example.superheroes.hero.ThanosHero.ID.equals(hero.getId())) {
			return com.example.superheroes.client.ThanosSkinTextures.textureFor(
					com.example.superheroes.client.ClientThanosState.maskFor(player.getUUID()));
		}
		return hero.getSkinTexture();
	}
}
