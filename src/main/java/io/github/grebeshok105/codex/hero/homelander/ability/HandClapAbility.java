package io.github.grebeshok105.codex.hero.homelander.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.hero.homelander.effect.HomelanderEffects;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import io.github.grebeshok105.codex.mechanic.effect.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Homelander's hand clap. Activation starts the authored clip on every client
 * ({@code CLAP}) and schedules the hit for {@link #IMPACT_TICKS} later — the
 * authored ~1.50 s hand-contact frame — where {@code CLAP_IMPACT} carries the
 * burst and the {@code homelander.hand_clap} sound. Range, cone, damage,
 * knockback and cooldown are unchanged; the impact only moved in time. Death,
 * hero-swap or a stun during the windup cancels the hit and broadcasts
 * {@code CLAP_CANCEL}; the cooldown still applies from activation.
 */
public final class HandClapAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("hand_clap");
	/** Activation → authored hand-contact frame (~1.50 s at 20 tps). */
	public static final int IMPACT_TICKS = 30;
	private static final double RANGE = 18.0;
	private static final double CONE_HALF_ANGLE_COS = Math.cos(Math.toRadians(45.0));
	private static final float DAMAGE = 20.0f;
	private static final double KNOCKBACK = 3.5;
	private static final int COOLDOWN_TICKS = 240;

	/**
	 * Owner uuid → impact game-time. Only {@link ClearOn#LEAVE} auto-drops:
	 * death and hero-clear must send {@code CLAP_CANCEL} first, so they come
	 * through {@link #cancelPending} on the lifecycle hooks instead.
	 */
	private static final OwnedSessionMap<UUID, Long> PENDING =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE));

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 50f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		Vec3 origin = player.getEyePosition();
		Vec3 forward = player.getViewVector(1f).normalize();
		PENDING.put(player.getUUID(), player.getUUID(),
				player.serverLevel().getGameTime() + IMPACT_TICKS);
		VfxFx.event(player, HomelanderVfxIds.CLAP, origin, origin.add(forward.scale(RANGE)), 1f);
		AbilityCooldowns.setCooldownTicks(player, ID, COOLDOWN_TICKS);
		return true;
	}

	/** Lands every pending clap whose impact tick arrived; drops cancelled ones. */
	public static void serverTick(MinecraftServer server) {
		if (PENDING.size() == 0) {
			return;
		}
		Iterator<Map.Entry<UUID, Long>> it = PENDING.iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Long> e = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			if (player.isDeadOrDying() || isStunned(player)) {
				it.remove();
				sendCancel(player);
				continue;
			}
			if (player.serverLevel().getGameTime() >= e.getValue()) {
				it.remove();
				impact(player);
			}
		}
	}

	/**
	 * Death / hero-clear hook: drop the pending hit and tell clients to release
	 * the clip early. No entry — no event (a cooldown re-cast is impossible).
	 */
	public static void cancelPending(ServerPlayer player) {
		if (PENDING.remove(player.getUUID()) != null) {
			sendCancel(player);
		}
	}

	private static boolean isStunned(ServerPlayer player) {
		return player.hasEffect(ModEffects.DISABLED_ABILITIES) || HomelanderEffects.isAftermath(player);
	}

	private static void sendCancel(ServerPlayer player) {
		Vec3 origin = player.getEyePosition();
		VfxFx.event(player, HomelanderVfxIds.CLAP_CANCEL, origin, origin, 1f);
	}

	private static void impact(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.getEyePosition();
		Vec3 forward = player.getViewVector(1f).normalize();

		AABB area = new AABB(origin, origin).inflate(RANGE);
		List<Entity> hits = level.getEntities(player, area,
				e -> e instanceof LivingEntity le && TargetFilters.hostileTo(player).test(le));
		for (Entity entity : hits) {
			Vec3 toTarget = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(origin);
			double dist = toTarget.length();
			if (dist > RANGE || dist < 0.001) continue;
			Vec3 norm = toTarget.normalize();
			if (norm.dot(forward) < CONE_HALF_ANGLE_COS) continue;
			entity.hurt(level.damageSources().playerAttack(player), DAMAGE);
			Vec3 push = forward.scale(KNOCKBACK);
			entity.push(push.x, 0.6, push.z);
			entity.hurtMarked = true;
		}

		VfxFx.event(player, HomelanderVfxIds.CLAP_IMPACT, origin, origin.add(forward.scale(RANGE)), 1f);
	}
}
