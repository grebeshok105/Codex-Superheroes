package io.github.grebeshok105.codex.mechanic.falls;

import io.github.grebeshok105.codex.mechanic.flight.FlightController;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.hero.LandingImpact;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.model.HeroData;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class HeroLandingTracker {
	private static final float MIN_FALL_DISTANCE = 10.0f;
	private static final double TELEPORT_DETECT_DROP = 8.0;
	private static final long LANDING_COOLDOWN_TICKS = 5L;

	private static final OwnedSessionMap<UUID, State> states = OwnedSessionMap.create(
			LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.HERO_CLEAR));

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
		long lastLandingTick;
	}

	public static void reset(ServerPlayer player) {
		states.remove(player.getUUID());
	}

	private static void tickPlayer(ServerPlayer player, long now) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		if (!data.hasHero()) {
			states.remove(player.getUUID());
			return;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) {
			states.remove(player.getUUID());
			return;
		}

		State s = states.get(player.getUUID());
		if (s == null) {
			s = new State();
			s.peakY = player.getY();
			s.prevY = player.getY();
			s.prevX = player.getX();
			s.prevZ = player.getZ();
			s.lastDeltaY = 0.0;
			s.lastHorizontalSpeed = 0.0;
			s.wasOnGround = player.onGround();
			s.tracking = !player.onGround();
			s.lastLandingTick = 0L;
			states.put(player.getUUID(), player.getUUID(), s);
		}

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

		if (flying || hero.suppressesLanding(player)) {
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
				if (fallDist >= MIN_FALL_DISTANCE && now - s.lastLandingTick > LANDING_COOLDOWN_TICKS) {
					s.lastLandingTick = now;
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

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		tickPlayer(player, player.level().getGameTime());
	}

	public static void pruneGonePlayers(MinecraftServer server) {
		Iterator<Map.Entry<UUID, State>> it = states.iterator();
		while (it.hasNext()) {
			if (server.getPlayerList().getPlayer(it.next().getKey()) == null) {
				it.remove();
			}
		}
	}

}
