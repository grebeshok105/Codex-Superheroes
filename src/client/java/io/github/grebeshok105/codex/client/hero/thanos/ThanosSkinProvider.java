package io.github.grebeshok105.codex.client.hero.thanos;

import io.github.grebeshok105.codex.client.ClientThanosState;
import io.github.grebeshok105.codex.client.ThanosSkinTextures;
import io.github.grebeshok105.codex.client.core.render.SkinProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Перчатка показывает только собранные камни и на теле, и на руке от первого лица (маска синхронизирована). */
final class ThanosSkinProvider implements SkinProvider {
	@Override
	@Nullable
	public ResourceLocation skin(AbstractClientPlayer player, ResourceLocation heroId) {
		return ThanosSkinTextures.textureFor(ClientThanosState.maskFor(player.getUUID()));
	}

	@Override
	@Nullable
	public ResourceLocation handSkin(AbstractClientPlayer player, ResourceLocation heroId) {
		return ThanosSkinTextures.textureFor(ClientThanosState.maskFor(player.getUUID()));
	}
}
