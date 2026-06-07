package com.example.superheroes.client.mixin;

import com.example.superheroes.ModId;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientShadowArmyState;
import com.example.superheroes.client.ClientUraniumPressureState;
import com.example.superheroes.client.RemoteHeroSkins;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.hero.HomelanderHero;
import com.example.superheroes.hero.SungJinwooHero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerSkinMixin {
	@Unique
	private static final ResourceLocation WOUNDED_HOMELANDER = ModId.of("textures/entity/hero/infected_homelander_wounded.png");

	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void superheroes$forceHeroSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		ResourceLocation heroId = superheroes$heroIdFor(self);
		if (heroId == null) {
			return;
		}
		ResourceLocation heroTexture = superheroes$heroTexture(heroId);
		if (HomelanderHero.ID.equals(heroId) && ClientUraniumPressureState.isPressured(self.getUUID())) {
			heroTexture = WOUNDED_HOMELANDER;
		}
		if (SungJinwooHero.ID.equals(heroId) && ClientShadowArmyState.hasShadows(self.getUUID())) {
			heroTexture = SungJinwooHero.SKIN_PHASE_2;
		}
		PlayerSkin orig = cir.getReturnValue();
		cir.setReturnValue(new PlayerSkin(
				heroTexture != null ? heroTexture : DefaultPlayerSkin.getDefaultTexture(),
				null,
				null,
				null,
				PlayerSkin.Model.WIDE,
				orig != null && orig.secure()
		));
	}

	@Unique
	private static ResourceLocation superheroes$heroIdFor(AbstractClientPlayer player) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
			return ClientHeroState.data().hasHero() ? ClientHeroState.data().heroId() : null;
		}
		return RemoteHeroSkins.get(player.getUUID());
	}

	@Unique
	private static ResourceLocation superheroes$heroTexture(ResourceLocation heroId) {
		Hero hero = Heroes.get(heroId);
		return hero != null ? hero.getSkinTexture() : null;
	}
}
