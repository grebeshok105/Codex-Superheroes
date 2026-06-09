package com.example.superheroes.effect;

import com.example.superheroes.ModId;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.AttributeModifierSet;
import com.example.superheroes.hero.BattleBeastHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BattleBeastCurseController {
	private static final long STEP_TICKS = 30L * 20L;
	public static final int MAX_STAGE = 10;
	private static final double BASE_ARMOR = 20.0;
	private static final double BASE_TOUGHNESS = 8.0;
	private static final double BASE_DAMAGE = 8.0;
	private static final double BASE_ATTACK_SPEED = 0.4;
	private static final double BASE_SPEED = 0.16;
	private static final double BASE_HEALTH = 30.0;
	private static final double BASE_REACH = 0.8;
	private static final double BASE_STEP = 0.5;

	private static final ResourceLocation CURSE_ARMOR = ModId.of("modifiers/battle_beast/curse_armor");
	private static final ResourceLocation CURSE_TOUGHNESS = ModId.of("modifiers/battle_beast/curse_toughness");
	private static final ResourceLocation CURSE_DAMAGE = ModId.of("modifiers/battle_beast/curse_damage");
	private static final ResourceLocation CURSE_ATTACK_SPEED = ModId.of("modifiers/battle_beast/curse_attack_speed");
	private static final ResourceLocation CURSE_SPEED = ModId.of("modifiers/battle_beast/curse_speed");
	private static final ResourceLocation CURSE_HEALTH = ModId.of("modifiers/battle_beast/curse_max_health");
	private static final ResourceLocation CURSE_REACH = ModId.of("modifiers/battle_beast/curse_reach");
	private static final ResourceLocation CURSE_STEP = ModId.of("modifiers/battle_beast/curse_step_height");

	private static final Map<UUID, Long> START_TICKS = new HashMap<>();
	private static final Map<UUID, Integer> STAGES = new HashMap<>();

	private BattleBeastCurseController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (isBattleBeast(player)) {
					tick(player);
				} else if (START_TICKS.containsKey(player.getUUID()) || STAGES.containsKey(player.getUUID())) {
					clear(player);
				}
			}
		});
	}

	public static void clear(ServerPlayer player) {
		removeCurse(player);
		healAfterStageChange(player, 0);
		START_TICKS.remove(player.getUUID());
		STAGES.remove(player.getUUID());
	}

	public static int setStage(ServerPlayer player, int stage) {
		int clamped = clampStage(stage);
		UUID id = player.getUUID();
		long now = player.serverLevel().getGameTime();
		START_TICKS.put(id, now - clamped * STEP_TICKS);
		removeCurse(player);
		if (clamped > 0) {
			buildCurseSet(clamped).apply(player);
		}
		STAGES.put(id, clamped);
		healAfterStageChange(player, clamped);
		refreshEffects(player, clamped);
		if (clamped > 0) {
			announceStage(player, clamped);
		}
		return clamped;
	}

	public static float scaleDamage(ServerPlayer player, float baseDamage) {
		return baseDamage * damageMultiplier(player);
	}

	public static float damageMultiplier(ServerPlayer player) {
		int stage = STAGES.getOrDefault(player.getUUID(), 0);
		if (stage <= 0) {
			return 1.0f;
		}
		return Math.min(5.5f, 1.0f + stage * 0.34f + Math.max(0, stage - 4) * 0.18f);
	}

	private static void tick(ServerPlayer player) {
		UUID id = player.getUUID();
		long now = player.serverLevel().getGameTime();
		if (!START_TICKS.containsKey(id)) {
			START_TICKS.put(id, now);
			removeCurse(player);
		}
		int stage = Math.min(MAX_STAGE, (int) ((now - START_TICKS.get(id)) / STEP_TICKS));
		int oldStage = STAGES.getOrDefault(id, -1);
		if (stage != oldStage) {
			removeCurse(player);
			if (stage > 0) {
				buildCurseSet(stage).apply(player);
			}
			STAGES.put(id, stage);
			if (stage > 0) {
				announceStage(player, stage);
			}
			healAfterStageChange(player, stage);
		}
		refreshEffects(player, stage);
		ambientFx(player, stage);
	}

	private static void refreshEffects(ServerPlayer player, int stage) {
		if (stage >= 2 && player.tickCount % 40 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 90,
					Math.min(2, stage / 4), true, false, true));
		}
		if (stage >= 3 && player.tickCount % 40 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 90,
					Math.min(2, stage / 4), true, false, true));
		}
		if (stage >= 5 && player.tickCount % 60 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 110,
					Math.min(3, Math.max(1, stage / 4)), true, false, true));
		}
		if (stage >= 6 && player.tickCount % 80 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100,
					Math.min(2, (stage - 4) / 3), true, false, true));
		}
	}

	private static void ambientFx(ServerPlayer player, int stage) {
		if (stage <= 0) return;
		ServerLevel level = player.serverLevel();
		if (player.tickCount % Math.max(3, 12 - stage) == 0) {
			level.sendParticles(ParticleTypes.CRIT,
					player.getX(), player.getY() + 1.1, player.getZ(),
					4 + stage * 2, 0.45, 0.7, 0.45, 0.07);
		}
		if (stage >= 4 && player.tickCount % 8 == 0) {
			level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
					player.getX(), player.getY() + 1.2, player.getZ(),
					2 + stage / 2, 0.4, 0.65, 0.4, 0.04);
		}
	}

	private static void announceStage(ServerPlayer player, int stage) {
		ServerLevel level = player.serverLevel();
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		int particles = 48 + stage * 16;
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, particles, 0.75, 1.1, 0.75, 0.14);
		level.sendParticles(ParticleTypes.CRIT, x, y, z, particles, 0.75, 0.95, 0.75, 0.22);
		level.playSound(null, x, y, z, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS,
				1.2f + stage * 0.15f, Math.max(0.35f, 1.05f - stage * 0.06f));
		if (stage >= 4) {
			level.playSound(null, x, y, z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8f, 1.4f);
			level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
		}
		player.displayClientMessage(Component.translatable("hero.superheroes.battle_beast.curse.stage", stage), true);
	}

	private static void removeCurse(ServerPlayer player) {
		buildCurseSet(0).remove(player);
	}

	private static AttributeModifierSet buildCurseSet(int stage) {
		Stats stats = statsFor(stage);
		return AttributeModifierSet.builder()
				.add(Attributes.ARMOR, CURSE_ARMOR, stats.armor - BASE_ARMOR, AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.ARMOR_TOUGHNESS, CURSE_TOUGHNESS, stats.toughness - BASE_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.ATTACK_DAMAGE, CURSE_DAMAGE, stats.damage - BASE_DAMAGE, AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.ATTACK_SPEED, CURSE_ATTACK_SPEED, stats.attackSpeed - BASE_ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.MOVEMENT_SPEED, CURSE_SPEED, stats.speed - BASE_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
				.add(Attributes.MAX_HEALTH, CURSE_HEALTH, stats.health - BASE_HEALTH, AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.ENTITY_INTERACTION_RANGE, CURSE_REACH, stats.reach - BASE_REACH, AttributeModifier.Operation.ADD_VALUE)
				.add(Attributes.STEP_HEIGHT, CURSE_STEP, stats.step - BASE_STEP, AttributeModifier.Operation.ADD_VALUE)
				.build();
	}

	private static Stats statsFor(int stage) {
		int s = clampStage(stage);
		return new Stats(
				BASE_ARMOR + s * 4.0,
				BASE_TOUGHNESS + s * 2.8,
				BASE_DAMAGE + s * 4.5,
				BASE_ATTACK_SPEED + s * 0.14,
				BASE_SPEED + s * 0.05,
				BASE_HEALTH + s * 15.0,
				BASE_REACH + s * 0.18,
				BASE_STEP + s * 0.1);
	}

	private static void healAfterStageChange(ServerPlayer player, int stage) {
		float health = player.getHealth();
		if (stage > 0 && health < player.getMaxHealth()) {
			health += 8f + stage * 4f;
		}
		player.setHealth(Math.min(player.getMaxHealth(), health));
	}

	private static int clampStage(int stage) {
		return Math.max(0, Math.min(MAX_STAGE, stage));
	}

	private static boolean isBattleBeast(Player player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return BattleBeastHero.ID.equals(data.heroId());
	}

	private record Stats(double armor, double toughness, double damage, double attackSpeed,
			double speed, double health, double reach, double step) {
	}
}
