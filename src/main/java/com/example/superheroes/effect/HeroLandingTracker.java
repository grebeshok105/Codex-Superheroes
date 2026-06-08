package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.hero.LandingImpact;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class HeroLandingTracker {
	private static final float MIN_FALL_DISTANCE = 10.0f;
	private static final double TELEPORT_DETECT_DROP = 8.0;
	private static final long LANDING_COOLDOWN_MS = 250L;

	private static final Map<UUID, State> states = new HashMap<>();

	private HeroLandingTracker() {
	}

	private static final class State {
		double peakY;
		double prevY;
		double prevX;
		double prevZ;
		double lastDeltaY;
		double lastHorizontalSpeed;
		boolean wasOnGround;
		boolean tracking;
		long lastLandingMs;
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = System.currentTimeMillis();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player, now);
			}
			Iterator<Map.Entry<UUID, State>> it = states.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, State> e = it.next();
				if (server.getPlayerList().getPlayer(e.getKey()) == null) {
					it.remove();
				}
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			states.remove(handler.getPlayer().getUUID());
		});
	}

	public static void reset(ServerPlayer player) {
		states.remove(player.getUUID());
	}

	private static void tickPlayer(ServerPlayer player, long now) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			states.remove(player.getUUID());
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) {
			states.remove(player.getUUID());
			return;
		}

		State s = states.computeIfAbsent(player.getUUID(), k -> {
			State ns = new State();
			ns.peakY = player.getY();
			ns.prevY = player.getY();
			ns.prevX = player.getX();
			ns.prevZ = player.getZ();
			ns.lastDeltaY = 0.0;
			ns.lastHorizontalSpeed = 0.0;
			ns.wasOnGround = player.onGround();
			ns.tracking = !player.onGround();
			ns.lastLandingMs = 0L;
			return ns;
		});

		double currentX = player.getX();
		double currentY = player.getY();
		double currentZ = player.getZ();
		boolean onGround = player.onGround();
		boolean flying = FlightController.isFlightActive(data);

		double drop = s.prevY - currentY;
		double dx = currentX - s.prevX;
		double dz = currentZ - s.prevZ;
		double horizontalSpeed = Math.sqrt(dx * dx + dz * dz);

		if (drop > TELEPORT_DETECT_DROP || -drop > TELEPORT_DETECT_DROP) {
			s.peakY = currentY;
			s.tracking = !onGround;
			s.wasOnGround = onGround;
			s.prevX = currentX;
			s.prevY = currentY;
			s.prevZ = currentZ;
			s.lastDeltaY = 0.0;
			s.lastHorizontalSpeed = 0.0;
			return;
		}

		if (flying || UnibeamController.isBusy(player)) {
			s.peakY = currentY;
			s.tracking = !onGround;
			s.wasOnGround = onGround;
			s.prevX = currentX;
			s.prevY = currentY;
			s.prevZ = currentZ;
			s.lastDeltaY = -drop;
			s.lastHorizontalSpeed = horizontalSpeed;
			return;
		}

		if (!onGround) {
			if (!s.tracking) {
				s.peakY = currentY;
				s.tracking = true;
			} else if (currentY > s.peakY) {
				s.peakY = currentY;
			}
		} else {
			if (!s.wasOnGround && s.tracking) {
				float fallDist = (float) Math.max(0.0, s.peakY - currentY);
				if (fallDist >= MIN_FALL_DISTANCE && now - s.lastLandingMs > LANDING_COOLDOWN_MS) {
					s.lastLandingMs = now;
					double vSpeed = Math.abs(s.lastDeltaY);
					double hSpeed = s.lastHorizontalSpeed;
					LandingImpact impact = LandingImpact.compute(fallDist, vSpeed, hSpeed);
					hero.onLanded(player, impact);
				}
				s.tracking = false;
				s.peakY = currentY;
			}
		}

		s.wasOnGround = onGround;
		s.prevX = currentX;
		s.prevY = currentY;
		s.prevZ = currentZ;
		s.lastDeltaY = -drop;
		s.lastHorizontalSpeed = horizontalSpeed;
	}
}
