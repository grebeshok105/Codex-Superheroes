package com.example.superheroes.ability;

import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.effect.ThanosCrossModSnapHook;
import com.example.superheroes.effect.ThanosGauntletStateController;
import com.example.superheroes.effect.ThanosSnapWindupController;
import com.example.superheroes.item.infinity.InfinityStoneItem;
import com.example.superheroes.item.infinity.InfinityStoneType;
import net.minecraft.world.item.ItemStack;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public final class ThanosSnapAbility implements Ability {
	private static final int COOLDOWN_TICKS = 1800;
	private static final double RADIUS = 128.0;
	private static final int DURATION_TICKS = 200;
	private static final int WINDUP_TOTAL_TICKS = 115;
	private static final int WINDUP_SNAP_AT_TICK = 95;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.THANOS_SNAP;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 350f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		if (AbilityCooldowns.isOnCooldown(player, getId())) {
			return false;
		}
		EnumSet<InfinityStoneType> stones = ThanosGauntletStateController.getCurrentStones(player);
		if (stones.size() < InfinityStoneType.values().length) {
			player.displayClientMessage(
					Component.translatable("ability.superheroes.thanos_snap.gate_failed",
									stones.size(), InfinityStoneType.values().length)
							.withStyle(ChatFormatting.LIGHT_PURPLE),
					true);
			return false;
		}
		return true;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		if (ThanosSnapWindupController.isWindingUp(player)) {
			return false;
		}
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.THANOS_SNAP_VOICE, SoundSource.PLAYERS, 1.6f, 1.0f);
		ThanosSnapWindupController.schedule(player, WINDUP_SNAP_AT_TICK, WINDUP_TOTAL_TICKS);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void executeSnap(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		double cx = player.getX();
		double cy = player.getY();
		double cz = player.getZ();

		AABB aoe = player.getBoundingBox().inflate(RADIUS, RADIUS, RADIUS);
		List<Player> victims = level.getEntitiesOfClass(Player.class, aoe,
				p -> p.isAlive() && !p.getUUID().equals(player.getUUID())
						&& !p.isCreative() && !p.isSpectator());

		int snapped = 0;
		for (Player victim : victims) {
			victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_TICKS, 4, false, true, true));
			victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DURATION_TICKS, 4, false, true, true));
			victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, DURATION_TICKS, 4, false, true, true));
			victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DURATION_TICKS, 0, false, true, true));
			victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DURATION_TICKS, 0, false, true, true));
			victim.addEffect(new MobEffectInstance(ModEffects.SNAPPED, DURATION_TICKS, 0, false, true, true));
			victim.addEffect(new MobEffectInstance(ModEffects.DISABLED_ABILITIES, DURATION_TICKS, 0, false, true, true));
			victim.addEffect(new MobEffectInstance(ModEffects.HEAL_BLOCK, DURATION_TICKS, 0, false, true, true));
			ThanosCrossModSnapHook.revokeHero(victim);
			level.sendParticles(ModParticles.PURPLE_FLAME,
					victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
					60, 0.5, 1.0, 0.5, 0.06);
			level.sendParticles(ModParticles.DARK_STAR,
					victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(),
					40, 0.5, 0.8, 0.5, 0.04);
			level.sendParticles(ParticleTypes.LARGE_SMOKE,
					victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
					25, 0.5, 0.6, 0.5, 0.02);
			level.sendParticles(ModParticles.SOUL_SPARK,
					victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
					80, 0.6, 1.0, 0.6, 0.10);
			level.sendParticles(ModParticles.NIGHTFALL,
					victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
					40, 0.5, 0.8, 0.5, 0.04);
			level.sendParticles(ModParticles.CHAOS_ORB,
					victim.getX(), victim.getY() + victim.getBbHeight() * 0.7, victim.getZ(),
					12, 0.4, 0.5, 0.4, 0.02);
			snapped++;
		}

		level.sendParticles(ParticleTypes.FLASH,
				cx, cy + 2.0, cz, 30, 6.0, 5.0, 6.0, 0.0);
		level.sendParticles(ModParticles.PURPLE_FLAME,
				cx, cy + 1.5, cz, 600, 12.0, 6.0, 12.0, 0.4);
		level.sendParticles(ModParticles.DARK_STAR,
				cx, cy + 1.5, cz, 400, 12.0, 6.0, 12.0, 0.3);
		level.sendParticles(ModParticles.BLACK_FLAME,
				cx, cy + 1.5, cz, 300, 10.0, 5.0, 10.0, 0.25);
		level.sendParticles(ModParticles.WHITE_BOOM,
				cx, cy + 2.0, cz, 80, 6.0, 4.0, 6.0, 0.0);
		level.sendParticles(ModParticles.SOUL_SPARK,
				cx, cy + 2.0, cz, 300, 12.0, 5.0, 12.0, 0.25);
		level.sendParticles(ModParticles.NIGHTFALL,
				cx, cy + 1.6, cz, 250, 11.0, 5.0, 11.0, 0.2);
		level.sendParticles(ModParticles.CHAOS_ORB,
				cx, cy + 2.5, cz, 120, 9.0, 4.0, 9.0, 0.15);
		level.sendParticles(ModParticles.SUN_PARTICLE,
				cx, cy + 2.0, cz, 80, 6.0, 3.0, 6.0, 0.1);

		for (int ring = 0; ring < 12; ring++) {
			double r = ring * 1.5 + 1.0;
			int count = (int) Math.min(64, r * 4);
			for (int i = 0; i < count; i++) {
				double a = (i / (double) count) * Math.PI * 2.0;
				double rx = Math.cos(a) * r;
				double rz = Math.sin(a) * r;
				level.sendParticles(ModParticles.PURPLE_FLAME,
						cx + rx, cy + 0.4, cz + rz,
						1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		level.playSound(null, cx, cy, cz,
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4.0f, 0.4f);
		level.playSound(null, cx, cy, cz,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0f, 0.25f);
		level.playSound(null, cx, cy, cz,
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0f, 0.5f);
		level.playSound(null, cx, cy, cz,
				SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 2.0f, 0.4f);
		level.playSound(null, cx, cy, cz,
				SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 1.6f);

		player.displayClientMessage(
				Component.translatable("ability.superheroes.thanos_snap.snapped", snapped)
						.withStyle(ChatFormatting.LIGHT_PURPLE),
				false);

		consumeGauntletAndStones(player);
	}

	private static void consumeGauntletAndStones(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		double cx = player.getX();
		double cy = player.getY() + 1.0;
		double cz = player.getZ();

		boolean removed = false;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.isEmpty()) continue;
			if (stack.getItem() instanceof InfinityStoneItem) {
				player.getInventory().setItem(i, ItemStack.EMPTY);
				removed = true;
			}
		}
		if (!removed) return;

		ThanosGauntletStateController.sendStones(player, java.util.EnumSet.noneOf(InfinityStoneType.class));
		com.example.superheroes.hero.HeroAttributes.thanosClearStoneModifiers(player);

		level.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 6, 0.4, 0.4, 0.4, 0.0);
		level.sendParticles(ModParticles.WHITE_BOOM, cx, cy, cz, 200, 2.5, 2.0, 2.5, 0.0);
		level.sendParticles(ModParticles.PURPLE_FLAME, cx, cy, cz, 400, 3.0, 2.0, 3.0, 0.4);
		level.sendParticles(ModParticles.SOUL_SPARK, cx, cy, cz, 250, 2.5, 2.0, 2.5, 0.3);
		level.sendParticles(ModParticles.CHAOS_ORB, cx, cy, cz, 80, 2.0, 1.5, 2.0, 0.2);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, 60, 1.5, 1.0, 1.5, 0.05);

		level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0f, 0.8f);
		level.playSound(null, cx, cy, cz, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 2.5f, 0.5f);
		level.playSound(null, cx, cy, cz, SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 2.0f, 0.7f);

		player.displayClientMessage(
				Component.translatable("ability.superheroes.thanos_snap.stones_consumed")
						.withStyle(ChatFormatting.DARK_PURPLE),
				false);
	}
}
