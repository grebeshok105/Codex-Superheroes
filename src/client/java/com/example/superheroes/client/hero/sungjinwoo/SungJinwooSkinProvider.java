package com.example.superheroes.client.hero.sungjinwoo;

import com.example.superheroes.client.ClientShadowArmyState;
import com.example.superheroes.client.core.render.SkinProvider;
import com.example.superheroes.hero.SungJinwooHero;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Второй скин Сун Джин Ву. Условия намеренно разные, см. CL4:
 * {@link #skin} (тело, видно всем) повторяет старую проверку {@code hasShadows},
 * {@link #handSkin} (рука от первого лица) — {@code isPhase2}. Расхождение
 * существовало до миграции и сохранено как есть; выравнивание — отдельное
 * решение владельца героя.
 */
final class SungJinwooSkinProvider implements SkinProvider {
	@Override
	@Nullable
	public ResourceLocation skin(AbstractClientPlayer player, ResourceLocation heroId) {
		return ClientShadowArmyState.hasShadows(player.getUUID()) ? SungJinwooHero.SKIN_PHASE_2 : null;
	}

	@Override
	@Nullable
	public ResourceLocation handSkin(AbstractClientPlayer player, ResourceLocation heroId) {
		return ClientShadowArmyState.isPhase2(player.getUUID()) ? SungJinwooHero.SKIN_PHASE_2 : null;
	}
}
