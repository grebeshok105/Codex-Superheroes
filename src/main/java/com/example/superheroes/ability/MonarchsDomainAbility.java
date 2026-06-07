package com.example.superheroes.ability;

import com.example.superheroes.effect.MonarchsDomainController;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Monarch's Domain (Ult) — 10с домена монарха:
 *  - враги в радиусе 25 блоков получают 4 HP каждые 10 тиков (игнор брони)
 *  - Сон получает Resistance II + KB-immunity
 *  - после: Weakness II + Slowness 30% на 100t
 *
 *  - Cost: 80 MANA (по дефолту биндится на mana)
 *  - CD: 3600t (180с)
 */
public final class MonarchsDomainAbility implements Ability {
	public static final int DURATION_TICKS = 200; // 10s
	public static final int COOLDOWN_TICKS = 3600;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.MONARCHS_DOMAIN;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 80f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 1, true, true, true));

		MonarchsDomainController.activate(player, DURATION_TICKS);
		com.example.superheroes.effect.SungJinwooController.enterPhase2(player);

		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1, player.getZ(),
				200, 1.5, 1.5, 1.5, 0.2);
		level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(),
				300, 4.0, 2.0, 4.0, 0.6);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.4f, 0.7f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.7f);

		AbilityCooldowns.setCooldownTicks(player, AbilityIds.MONARCHS_DOMAIN, COOLDOWN_TICKS);
		return true;
	}
}
