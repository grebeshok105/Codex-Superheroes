package com.example.superheroes.client.hero.ironman;

import com.example.superheroes.ability.ironman.IronManSuitVariant;
import com.example.superheroes.client.ClientSuitVariantState;
import com.example.superheroes.client.core.render.SkinProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Текущий вариант костюма синхронизирован со всеми клиентами — скин берётся из него. */
final class IronManSkinProvider implements SkinProvider {
	@Override
	@Nullable
	public ResourceLocation skin(AbstractClientPlayer player, ResourceLocation heroId) {
		int variant = ClientSuitVariantState.variantFor(player.getUUID());
		return IronManSuitVariant.get(variant).texture();
	}
}
