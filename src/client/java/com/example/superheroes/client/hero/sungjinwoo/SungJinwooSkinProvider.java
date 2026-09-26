package com.example.superheroes.client.hero.sungjinwoo;

import com.example.superheroes.client.ClientShadowArmyState;
import com.example.superheroes.client.core.render.SkinProvider;
import com.example.superheroes.hero.SungJinwooHero;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Второй скин Сун Джин Ву. Два условия намеренно разные, см. CL4:
 * {@link #skin} повторяет проверку тела скина ({@code hasShadows}),
 * {@link #slimModel} повторяет проверку рендера руки от первого лица ({@code isPhase2}).
 */
final class SungJinwooSkinProvider implements SkinProvider {
	@Override
	@Nullable
	public ResourceLocation skin(AbstractClientPlayer player, ResourceLocation heroId) {
		return ClientShadowArmyState.hasShadows(player.getUUID()) ? SungJinwooHero.SKIN_PHASE_2 : null;
	}

	@Override
	@Nullable
	public Boolean slimModel(AbstractClientPlayer player, ResourceLocation heroId) {
		return ClientShadowArmyState.isPhase2(player.getUUID());
	}
}
