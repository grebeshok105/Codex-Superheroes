package com.example.superheroes.ability;

import com.example.superheroes.hero.HeroAttributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Hulkbuster — Тони активирует тяжёлый броне-костюм. Пока активен:
 *  — масштаб игрока +20%, +25 брони, +10 toughness, +6 урона, +20 max HP,
 *  — Resistance II (доп. снижение урона), но Slowness I (-10% от и так замедленного MS),
 *  — visual: пар/искры вокруг.
 * Расход энергии: 4/тик (~80/сек). При выходе — атрибуты снимаются, эффекты тоже.
 */
public final class IronManHulkbusterAbility implements Ability {
	private static final int EFFECT_DURATION = 40;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.IRON_MAN_HULKBUSTER;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 4f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		HeroAttributes.HULKBUSTER.apply(player);
		applyEffects(player);
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.6f, 0.7f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 1.4f);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.2, player.getZ(),
				40, 0.8, 1.0, 0.8, 0.05);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		applyEffects(player);
		if (player.tickCount % 20 == 0) {
			ServerLevel level = player.serverLevel();
			level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(),
					6, 0.5, 0.6, 0.5, 0.02);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.8, player.getZ(),
					3, 0.5, 0.5, 0.5, 0.05);
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		HeroAttributes.HULKBUSTER.remove(player);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.IRON_GOLEM_DEATH, SoundSource.PLAYERS, 0.8f, 1.4f);
	}

	private static void applyEffects(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION, 0, true, false, true));
	}
}
