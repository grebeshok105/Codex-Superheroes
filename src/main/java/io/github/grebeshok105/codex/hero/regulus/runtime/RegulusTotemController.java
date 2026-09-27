package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.EnumSet;
import java.util.UUID;

public final class RegulusTotemController {
	private static final ResourceLocation REGULUS_ID = ModId.of("regulus");

	/** First-death ward is per-hero-session: survives death itself, cleared on hero clear. */
	private static final OwnedSessionMap<UUID, Boolean> TOTEM_USED = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.of(ClearOn.HERO_CLEAR));

	private RegulusTotemController() {
	}

	public static void register(HeroModuleContext ctx) {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return true;
			}
			HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
			if (!data.hasHero() || !REGULUS_ID.equals(data.heroId())) {
				return true;
			}
			if (TOTEM_USED.containsKey(player.getUUID())) {
				if (RegulusMadnessController.consumeBonusLife(player)) {
					player.setHealth(player.getMaxHealth() * 0.5f);
					player.removeAllEffects();
					player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
					player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
					player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
					return false;
				}
				return true;
			}
			TOTEM_USED.put(player.getUUID(), player.getUUID(), Boolean.TRUE);
			player.setHealth(player.getMaxHealth());
			player.removeAllEffects();
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
			player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
			ServerLevel level = player.serverLevel();
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
					player.getX(), player.getY() + 1.0, player.getZ(),
					60, 0.4, 0.6, 0.4, 0.3);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1f);
			return false;
		});
	}

	public static void clear(UUID playerId) {
		TOTEM_USED.remove(playerId);
	}

	public static boolean wasUsed(UUID playerId) {
		return TOTEM_USED.containsKey(playerId);
	}
}
