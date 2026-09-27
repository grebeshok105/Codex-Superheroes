package io.github.grebeshok105.codex.client.hero.homelander;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientUraniumPressureState;
import io.github.grebeshok105.codex.client.core.render.SkinProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Раненый Хоумлендер: пока активно урановое давление, скин подменяется на wounded-текстуру. */
final class HomelanderSkinProvider implements SkinProvider {
	private static final ResourceLocation WOUNDED = ModId.of("textures/entity/hero/infected_homelander_wounded.png");

	@Override
	@Nullable
	public ResourceLocation skin(AbstractClientPlayer player, ResourceLocation heroId) {
		return ClientUraniumPressureState.isPressured(player.getUUID()) ? WOUNDED : null;
	}
}
