package io.github.grebeshok105.codex.mixin;

import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.effect.RegulusMadnessController;
import io.github.grebeshok105.codex.effect.SuperJumpController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFallDamageMixin {
	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	private void superheroes$cancelFallDamage(float fallDistance, float multiplier, DamageSource source,
			CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player player)) {
			return;
		}
		boolean counterActive = RegulusMadnessController.isCounterInvolved(player);
		if (SuperJumpController.hasFallImmunity(player) && !counterActive) {
			cir.setReturnValue(false);
			return;
		}
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) {
			return;
		}
		if (hero.cancelsFallDamage(player) && !counterActive) {
			cir.setReturnValue(false);
		}
	}
}
