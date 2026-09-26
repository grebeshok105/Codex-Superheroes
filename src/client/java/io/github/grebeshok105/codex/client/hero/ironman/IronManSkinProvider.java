package io.github.grebeshok105.codex.client.hero.ironman;

import io.github.grebeshok105.codex.ability.ironman.IronManSuitVariant;
import io.github.grebeshok105.codex.client.ClientSuitVariantState;
import io.github.grebeshok105.codex.client.core.render.SkinProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Вариант костюма синхронизирован со всеми клиентами — и тело, и рука от первого лица берут его текстуру. */
final class IronManSkinProvider implements SkinProvider {
	@Override
	@Nullable
	public ResourceLocation skin(AbstractClientPlayer player, ResourceLocation heroId) {
		int variant = ClientSuitVariantState.variantFor(player.getUUID());
		return IronManSuitVariant.get(variant).texture();
	}

	@Override
	@Nullable
	public ResourceLocation handSkin(AbstractClientPlayer player, ResourceLocation heroId) {
		int variant = ClientSuitVariantState.variantFor(player.getUUID());
		return IronManSuitVariant.get(variant).texture();
	}
}
