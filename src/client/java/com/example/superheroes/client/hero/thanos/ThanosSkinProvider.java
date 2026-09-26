package com.example.superheroes.client.hero.thanos;

import com.example.superheroes.client.ClientThanosState;
import com.example.superheroes.client.ThanosSkinTextures;
import com.example.superheroes.client.core.render.SkinProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Перчатка на скине показывает только реально собранные камни (маски синхронизированы со всеми клиентами). */
final class ThanosSkinProvider implements SkinProvider {
	@Override
	@Nullable
	public ResourceLocation skin(AbstractClientPlayer player, ResourceLocation heroId) {
		return ThanosSkinTextures.textureFor(ClientThanosState.maskFor(player.getUUID()));
	}
}
