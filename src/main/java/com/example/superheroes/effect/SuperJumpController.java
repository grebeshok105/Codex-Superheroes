package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.hero.KratosHero;
import com.example.superheroes.hero.NarutoHero;
import com.example.superheroes.hero.RegulusHero;
import com.example.superheroes.hero.ReinhardHero;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.transform.HeroData;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SuperJumpController {
	private static final double JUMP_VELOCITY = 2.7;
	private static final int COOLDOWN_TICKS = 40;
	private static final int IMMUNITY_LIFE_TICKS = 400;

	private static final Set<ResourceLocation> ALLOWED_HEROES = Set.of(
			RegulusHero.ID,
			DoomsdayHero.ID,
			KratosHero.ID,
			ThanosHero.ID,
			NarutoHero.ID,
			ReinhardHero.ID
	);

	private static final Map<UUID, Long> COOLDOWN = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> FALL_IMMUNITY_UNTIL = new ConcurrentHashMap<>();

	private SuperJumpController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Long until = FALL_IMMUNITY_UNTIL.get(player.getUUID());
				if (until == null) {
					continue;
				}
				if (player.level().getGameTime() >= until) {
					FALL_IMMUNITY_UNTIL.remove(player.getUUID());
				} else if (player.onGround() && player.getDeltaMovement().y <= 0.0) {
					player.fallDistance = 0f;
					FALL_IMMUNITY_UNTIL.remove(player.getUUID());
				}
			}
		});
	}

	public static void activate(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		if (!ALLOWED_HEROES.contains(data.heroId())) {
			return;
		}
		UUID id = player.getUUID();
		long now = player.level().getGameTime();
		Long ready = COOLDOWN.get(id);
		if (ready != null && now < ready) {
			return;
		}
		COOLDOWN.put(id, now + COOLDOWN_TICKS);
		FALL_IMMUNITY_UNTIL.put(id, now + IMMUNITY_LIFE_TICKS);

		Vec3 v = player.getDeltaMovement();
		player.setDeltaMovement(v.x, JUMP_VELOCITY, v.z);
		player.hurtMarked = true;
		player.fallDistance = 0f;

		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.4f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.7f);
		level.sendParticles(ParticleTypes.CLOUD,
				player.getX(), player.getY(), player.getZ(),
				40, 0.6, 0.05, 0.6, 0.4);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				player.getX(), player.getY(), player.getZ(),
				20, 0.8, 0.05, 0.8, 0.05);
		level.sendParticles(ParticleTypes.FIREWORK,
				player.getX(), player.getY() + 0.1, player.getZ(),
				25, 0.6, 0.1, 0.6, 0.2);
	}

	public static boolean hasFallImmunity(Player player) {
		Long until = FALL_IMMUNITY_UNTIL.get(player.getUUID());
		return until != null && player.level().getGameTime() < until;
	}

	public static void clear(UUID id) {
		COOLDOWN.remove(id);
		FALL_IMMUNITY_UNTIL.remove(id);
	}
}
