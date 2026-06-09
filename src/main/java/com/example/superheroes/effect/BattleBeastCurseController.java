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
	private static final int MAX_STAGE = 10;
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
		START_TICKS.remove(player.getUUID());
		STAGES.remove(player.getUUID());
	}

	public static float scaleDamage(ServerPlayer player, float baseDamage) {
		return baseDamage * damageMultiplier(player);
	}

	public static float damageMultiplier(ServerPlayer player) {
		int stage = STAGES.getOrDefault(player.getUUID(), 0);
		return switch (stage) {
			case 0 -> 1.0f;
			case 1 -> 1.15f;
			case 2 -> 1.30f;
			case 3 -> 1.50f;
			case 4 -> 2.0f;
			default -> Math.min(4.8f, 2.35f + (stage - 5) * 0.35f);
		};
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
			if (player.getHealth() < player.getMaxHealth()) {
				player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 4f + stage * 2f));
			}
		}
		refreshEffects(player, stage);
		ambientFx(player, stage);
	}

	private static void refreshEffects(ServerPlayer player, int stage) {
		if (stage >= 4 && player.tickCount % 40 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, true, false, true));
		}
		if (stage >= 5 && player.tickCount % 80 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1, true, false, true));
		}
	}

	private static void ambientFx(ServerPlayer player, int stage) {
		if (stage <= 0) return;
		ServerLevel level = player.serverLevel();
		if (player.tickCount % Math.max(4, 14 - stage) == 0) {
			level.sendParticles(ParticleTypes.CRIT,
					player.getX(), player.getY() + 1.1, player.getZ(),
					2 + stage, 0.35, 0.55, 0.35, 0.05);
		}
		if (stage >= 4 && player.tickCount % 10 == 0) {
			level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
					player.getX(), player.getY() + 1.2, player.getZ(),
					2, 0.3, 0.5, 0.3, 0.03);
		}
	}

	private static void announceStage(ServerPlayer player, int stage) {
		ServerLevel level = player.serverLevel();
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		int particles = 28 + stage * 10;
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, particles, 0.55, 0.9, 0.55, 0.12);
		level.sendParticles(ParticleTypes.CRIT, x, y, z, particles, 0.55, 0.8, 0.55, 0.18);
		level.playSound(null, x, y, z, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS,
				1.0f + stage * 0.12f, Math.max(0.45f, 1.05f - stage * 0.06f));
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
		int s = Math.max(0, Math.min(MAX_STAGE, stage));
		return switch (s) {
			case 0 -> new Stats(20.0, 8.0, 8.0, 0.4, 0.16, 30.0, 0.8, 0.5);
			case 1 -> new Stats(22.0, 9.0, 9.5, 0.45, 0.18, 35.0, 0.8, 0.5);
			case 2 -> new Stats(24.0, 10.0, 10.5, 0.55, 0.22, 38.0, 0.8, 0.55);
			case 3 -> new Stats(26.0, 12.0, 12.0, 0.6, 0.25, 40.0, 0.8, 0.6);
			case 4 -> new Stats(30.0, 16.0, 16.0, 0.8, 0.32, 60.0, 0.9, 0.65);
			default -> {
				int extra = s - 5;
				yield new Stats(
						Math.min(40.0, 32.0 + extra * 2.0),
						Math.min(26.0, 18.0 + extra * 2.0),
						Math.min(40.0, 20.0 + extra * 4.0),
						Math.min(1.6, 1.0 + extra * 0.15),
						Math.min(0.48, 0.36 + extra * 0.03),
						Math.min(130.0, 70.0 + extra * 12.0),
						Math.min(1.5, 1.1 + extra * 0.1),
						Math.min(1.0, 0.8 + extra * 0.04));
			}
		};
	}

	private static boolean isBattleBeast(Player player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return BattleBeastHero.ID.equals(data.heroId());
	}

	private record Stats(double armor, double toughness, double damage, double attackSpeed,
			double speed, double health, double reach, double step) {
	}
}
