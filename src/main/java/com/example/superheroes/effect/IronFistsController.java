package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.ability.IronFistsAbility;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.entity.HomelanderBossEntity;
import com.example.superheroes.network.ScreenShakeS2CPayload;
import com.example.superheroes.physics.ShockwaveUtil;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * §6 Iron Fists — обновлённая логика:
 *  - При активации полная энергия уже потрачена (см. IronFistsAbility.tryActivate).
 *  - НЕТ авто-таргета и авто-дэша. Игрок сам ловит цель ЛКМ.
 *  - При ЛКМ-попадании по валидной цели (Player / HomelanderBoss / Monster / Golem):
 *      → дэш в сторону цели,
 *      → урон + кб,
 *      → шоквейв,
 *      → 40t (2c) cooldown между такими ЛКМ-выпадами.
 *  - Animals / Villagers — обычный удар, без выпада/шоквейва (нет «эффекта»).
 *  - Footsteps loop сохраняется — шум активной фазы.
 *  - Другие способности заблокированы (AbilityRouter уже проверяет isActive(IRON_FISTS)).
 */
public final class IronFistsController {
	private static final int LMB_COOLDOWN_TICKS = 40;
	private static final double DASH_FORCE = 0.85;
	private static final double DASH_LIFT = 0.15;
	private static final double SHOCKWAVE_RADIUS = 4.5;
	private static final float SHOCKWAVE_DAMAGE = 8.0f;
	private static final int LOOP_INTERVAL_TICKS = 100;
	private static final int AURA_INTERVAL_TICKS = 4;

	private static final Map<UUID, Long> ACTIVATE_TICK = new HashMap<>();
	private static final Map<UUID, Long> LAST_DASH = new HashMap<>();

	private IronFistsController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
				if (!data.isActive(AbilityIds.IRON_FISTS)) {
					if (ACTIVATE_TICK.remove(player.getUUID()) != null) {
						LAST_DASH.remove(player.getUUID());
					}
				}
			}
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide() || !(player instanceof ServerPlayer sp)) {
				return InteractionResult.PASS;
			}
			HeroData data = sp.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (!data.isActive(AbilityIds.IRON_FISTS)) {
				return InteractionResult.PASS;
			}
			if (!(entity instanceof LivingEntity target) || target == sp) {
				return InteractionResult.PASS;
			}
			boolean dashTarget = isDashTarget(target);

			ServerLevel level = sp.serverLevel();
			long now = sp.level().getGameTime();
			if (dashTarget) {
				Long last = LAST_DASH.get(sp.getUUID());
				if (last != null && (now - last) < LMB_COOLDOWN_TICKS) {
					return InteractionResult.FAIL;
				}
				LAST_DASH.put(sp.getUUID(), now);

				// dash в сторону цели
				Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.4, 0)
						.subtract(sp.position()).normalize();
				Vec3 newVel = toTarget.scale(DASH_FORCE).add(0.0, DASH_LIFT, 0.0);
				sp.setDeltaMovement(newVel);
				sp.hurtMarked = true;
				sp.fallDistance = 0f;
				sp.hasImpulse = true;
			}

			// удар + кб
			target.hurt(level.damageSources().playerAttack(sp), IronFistsAbility.MELEE_DAMAGE);
			Vec3 push = sp.getViewVector(1f).scale(IronFistsAbility.MELEE_KNOCKBACK);
			target.push(push.x, 0.45, push.z);
			target.hurtMarked = true;

			Vec3 hp = target.position().add(0, target.getBbHeight() * 0.5, 0);
			level.sendParticles(ParticleTypes.END_ROD, hp.x, hp.y, hp.z, 18, 0.3, 0.3, 0.3, 0.05);
			level.sendParticles(ParticleTypes.CRIT, hp.x, hp.y, hp.z, 14, 0.3, 0.3, 0.3, 0.2);
			level.playSound(null, target.getX(), target.getY(), target.getZ(),
					ModSounds.HOMELANDER_IRON_FISTS_IMPACT, SoundSource.PLAYERS, 1.0f, 1.0f);

			if (dashTarget) {
				ShockwaveUtil.detonate(sp, target.position(), SHOCKWAVE_RADIUS, SHOCKWAVE_DAMAGE, false);
			}

			if (target instanceof ServerPlayer victim) {
				ServerPlayNetworking.send(victim, new ScreenShakeS2CPayload(2.0f, 18));
			}

			sp.swing(hand);
			sp.resetAttackStrengthTicker();
			return InteractionResult.SUCCESS;
		});
	}

	/**
	 * Цели валидные для дэша/шоквейва: Player, HomelanderBoss, Monster, Golem (Iron/Snow).
	 * Animals/Villagers — нет (обычный удар).
	 */
	private static boolean isDashTarget(LivingEntity target) {
		if (target instanceof AbstractVillager) return false;
		if (target instanceof Animal) return false;
		if (target instanceof Player) return true;
		if (target instanceof HomelanderBossEntity) return true;
		if (target instanceof Monster) return true;
		if (target instanceof IronGolem) return true;
		if (target instanceof SnowGolem) return true;
		return false;
	}

	public static void markActivated(ServerPlayer player) {
		ACTIVATE_TICK.put(player.getUUID(), player.level().getGameTime());
		LAST_DASH.remove(player.getUUID());
	}

	public static void markDeactivated(ServerPlayer player) {
		ACTIVATE_TICK.remove(player.getUUID());
		LAST_DASH.remove(player.getUUID());
	}

	public static void tickActive(ServerPlayer player) {
		UUID id = player.getUUID();
		long now = player.level().getGameTime();
		Long started = ACTIVATE_TICK.get(id);
		if (started == null) {
			started = now;
			ACTIVATE_TICK.put(id, started);
		}
		long elapsed = now - started;
		if (elapsed >= IronFistsAbility.DURATION_TICKS) {
			AbilityRouter.deactivate(player, AbilityIds.IRON_FISTS);
			return;
		}

		ServerLevel level = player.serverLevel();
		Vec3 p = player.position();

		if (elapsed > 0 && elapsed % LOOP_INTERVAL_TICKS == 0) {
			level.playSound(null, p.x, p.y, p.z, ModSounds.HOMELANDER_IRON_FISTS_CHARGE,
					SoundSource.PLAYERS, 0.8f, 1.0f);
		}

		if (elapsed % AURA_INTERVAL_TICKS == 0) {
			spawnHandAura(player);
		}
	}

	private static void spawnHandAura(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 forward = player.getViewVector(1f).normalize();
		Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();
		Vec3 base = player.position().add(0.0, 1.05, 0.0).add(forward.scale(0.25));
		Vec3 leftHand = base.add(right.scale(-0.35));
		Vec3 rightHand = base.add(right.scale(0.35));
		level.sendParticles(ParticleTypes.END_ROD,
				leftHand.x, leftHand.y, leftHand.z, 2, 0.06, 0.06, 0.06, 0.0);
		level.sendParticles(ParticleTypes.END_ROD,
				rightHand.x, rightHand.y, rightHand.z, 2, 0.06, 0.06, 0.06, 0.0);
	}
}
