package com.example.superheroes.effect;

import com.example.superheroes.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class DoomGripController {
	private static final int TOTAL_TICKS = 70;
	private static final int LUNGE_END = 8;
	private static final int LOCK_END = 60;
	private static final float HIT_DAMAGE = 18f;
	private static final int HIT_INTERVAL = 10;

	private static final Map<UUID, GripState> ACTIVE = new WeakHashMap<>();

	private DoomGripController() {
	}

	public static void start(ServerPlayer doomsday, LivingEntity target) {
		boolean wasNoAi = false;
		if (target instanceof Mob mob) {
			wasNoAi = mob.isNoAi();
			mob.setNoAi(true);
		}
		ACTIVE.put(doomsday.getUUID(), new GripState(target, 0, wasNoAi));
		ServerLevel level = doomsday.serverLevel();

		Vec3 look = doomsday.getLookAngle().normalize();
		Vec3 lungePos = target.position().subtract(look.scale(1.5));
		doomsday.teleportTo(level, lungePos.x, lungePos.y, lungePos.z,
				java.util.Set.of(), doomsday.getYRot(), doomsday.getXRot());
		doomsday.connection.resetPosition();
		doomsday.setInvulnerable(true);

		level.playSound(null, doomsday.getX(), doomsday.getY(), doomsday.getZ(),
				SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.6f, 0.6f);
		level.sendParticles(ParticleTypes.PORTAL, doomsday.getX(), doomsday.getY() + 1, doomsday.getZ(),
				40, 0.6, 1.0, 0.6, 0.5);
	}

	public static void serverTick() {
		Iterator<Map.Entry<UUID, GripState>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, GripState> entry = it.next();
			GripState state = entry.getValue();
			ServerPlayer doomsday = state.findDoomsday(entry.getKey());
			if (doomsday == null || state.target == null || !state.target.isAlive() || state.target.isRemoved()) {
				if (doomsday != null) doomsday.setInvulnerable(false);
				if (state.target instanceof Mob mob) mob.setNoAi(state.wasNoAi);
				it.remove();
				continue;
			}
			ServerLevel level = doomsday.serverLevel();
			LivingEntity target = state.target;

			if (state.tick <= LUNGE_END) {
				// holding pose
			} else if (state.tick <= LOCK_END) {
				Vec3 look = doomsday.getLookAngle().normalize();
				Vec3 holdPos = doomsday.position()
						.add(look.x * 1.6, 1.0, look.z * 1.6);
				if (target instanceof ServerPlayer sp) {
					sp.connection.teleport(holdPos.x, holdPos.y, holdPos.z, sp.getYRot(), sp.getXRot());
					sp.stopFallFlying();
					sp.setNoActionTime(0);
					sp.connection.send(new ClientboundSetEntityMotionPacket(sp.getId(), Vec3.ZERO));
				} else if (target instanceof Mob mob) {
					mob.moveTo(holdPos.x, holdPos.y, holdPos.z, mob.getYRot(), mob.getXRot());
				} else {
					target.setPos(holdPos.x, holdPos.y, holdPos.z);
				}
				target.setDeltaMovement(Vec3.ZERO);
				target.fallDistance = 0;
				target.hurtMarked = true;
				target.stopUsingItem();
				target.removeEffect(MobEffects.LEVITATION);

				if ((state.tick - LUNGE_END) % HIT_INTERVAL == 0) {
					target.hurt(ModDamageTypes.doomsdayDoomGrip(level, doomsday), HIT_DAMAGE);
					level.playSound(null, target.getX(), target.getY(), target.getZ(),
							SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.4f, 0.7f);
					level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
							target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
							12, 0.4, 0.4, 0.4, 0.0);
				}
			} else {
				Vec3 look = doomsday.getLookAngle().normalize();
				Vec3 throwVec = new Vec3(look.x * 2.5, 0.7, look.z * 2.5);
				if (target instanceof Mob mob) mob.setNoAi(state.wasNoAi);
				target.setDeltaMovement(throwVec);
				target.hurtMarked = true;
				if (target instanceof ServerPlayer sp) {
					sp.connection.send(new ClientboundSetEntityMotionPacket(sp.getId(), throwVec));
				}
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));

				level.playSound(null, doomsday.getX(), doomsday.getY(), doomsday.getZ(),
						SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.5f);
				level.sendParticles(ParticleTypes.LARGE_SMOKE,
						target.getX(), target.getY() + 0.5, target.getZ(),
						50, 1.0, 0.5, 1.0, 0.1);

				doomsday.setInvulnerable(false);
				it.remove();
				continue;
			}
			state.tick++;
		}
	}

	public static boolean isGripping(ServerPlayer doomsday) {
		return ACTIVE.containsKey(doomsday.getUUID());
	}

	public static void clear(ServerPlayer doomsday) {
		GripState s = ACTIVE.remove(doomsday.getUUID());
		if (s != null) {
			doomsday.setInvulnerable(false);
			if (s.target instanceof Mob mob) mob.setNoAi(s.wasNoAi);
		}
	}

	private static final class GripState {
		final LivingEntity target;
		final boolean wasNoAi;
		int tick;

		GripState(LivingEntity target, int tick, boolean wasNoAi) {
			this.target = target;
			this.tick = tick;
			this.wasNoAi = wasNoAi;
		}

		ServerPlayer findDoomsday(UUID id) {
			if (target == null) return null;
			if (target.level() instanceof ServerLevel sl
					&& sl.getServer() != null) {
				Player p = sl.getServer().getPlayerList().getPlayer(id);
				return p instanceof ServerPlayer sp ? sp : null;
			}
			return null;
		}
	}
}
