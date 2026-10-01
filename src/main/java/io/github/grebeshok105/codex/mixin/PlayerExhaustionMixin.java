package io.github.grebeshok105.codex.mixin;

import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.model.HeroData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hero-agnostic gate on the hunger pipeline: while a hero reports
 * {@link Hero#blocksExhaustionWhile} (Regulus's lion-heart void), food exhaustion
 * never accumulates — hunger drain and the starve damage that follows are covered by
 * the same absolute defense as ordinary hits.
 */
@Mixin(Player.class)
public abstract class PlayerExhaustionMixin {
	@Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
	private void superheroes$voidFoodExhaustion(float exhaustion, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (!(self instanceof ServerPlayer player)) {
			return;
		}
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero != null && hero.blocksExhaustionWhile(player)) {
			ci.cancel();
		}
	}
}
