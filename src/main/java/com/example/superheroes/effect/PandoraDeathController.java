package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.PandoraHero;
import com.example.superheroes.network.PandoraDeathS2CPayload;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pandora — one-time cinematic death (#7).
 *
 * <p>The FIRST time Pandora would die (per hero pick) her death is cancelled and a
 * cinematic plays for her killer: time freezes, the world is repainted blood-red with
 * the killer's view smoothly turned toward Pandora, she rises in a spray of blood and
 * reappears right behind the killer to a child's giggle. From then on she is permanently
 * invulnerable (she never dies again) and keeps her whole inventory — until the hero is
 * re-picked, which arms the one-time death again.
 */
public final class PandoraDeathController {
	/** Pandora players who have already spent their one-time death → now immortal. */
	private static final Set<UUID> IMMORTAL = ConcurrentHashMap.newKeySet();

	private PandoraDeathController() {
	}

	private static boolean isPandora(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && PandoraHero.ID.equals(data.heroId());
	}

	/**
	 * Hook for {@code ServerLivingEntityEvents.ALLOW_DEATH}.
	 *
	 * @return {@code true} to allow the (non-Pandora / non-applicable) death to proceed,
	 *         {@code false} to cancel it (Pandora's one-time cinematic or her later immortality).
	 */
	public static boolean allowDeath(ServerPlayer player, DamageSource source) {
		if (!isPandora(player)) {
			return true;
		}
		UUID id = player.getUUID();
		boolean firstDeath = IMMORTAL.add(id); // add() == true means it wasn't there yet
		// Keep her alive and whole either way.
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		player.removeAllEffects();
		if (firstDeath) {
			playCinematic(player, source);
		}
		return false; // never actually die
	}

	/** Re-arm the one-time death whenever a hero is (re-)taken. */
	public static void resetOnHeroTaken(ServerPlayer player) {
		IMMORTAL.remove(player.getUUID());
	}

	public static void onPlayerDisconnect(ServerPlayer player) {
		// Keep immortality tied to the live session only; a fresh login re-arms via hero pick.
		IMMORTAL.remove(player.getUUID());
	}

	private static void playCinematic(ServerPlayer pandora, DamageSource source) {
		ServerLevel level = pandora.serverLevel();
		ServerPlayer killer = resolveKiller(source, pandora);

		// Blood spray at the spot she "fell".
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
				pandora.getX(), pandora.getY() + 1.0, pandora.getZ(), 80, 0.4, 0.8, 0.4, 0.2);
		level.sendParticles(ParticleTypes.CRIMSON_SPORE,
				pandora.getX(), pandora.getY() + 1.0, pandora.getZ(), 60, 0.6, 1.0, 0.6, 0.05);

		// Reappear right behind the killer (if any), facing their back.
		if (killer != null && killer.serverLevel() == level) {
			Vec3 behind = behindOf(killer);
			pandora.teleportTo(behind.x, behind.y, behind.z);
			pandora.setYRot(killer.getYRot());
			pandora.setXRot(0f);
			pandora.connection.resetPosition();
			level.sendParticles(ParticleTypes.REVERSE_PORTAL,
					behind.x, behind.y + 1.0, behind.z, 50, 0.3, 0.8, 0.3, 0.05);
		}

		// Child's giggle — drop a custom ogg at assets/superheroes/sounds/pandora/child_giggle.ogg.
		level.playSound(null, pandora.getX(), pandora.getY(), pandora.getZ(),
				ModSounds.PANDORA_CHILD_GIGGLE, SoundSource.HOSTILE, 1.0f, 1.0f);

		// Drive the killer's client cinematic (camera turn + red repaint + fade). Pandora sees it too.
		PandoraDeathS2CPayload payload = new PandoraDeathS2CPayload(
				pandora.getX(), pandora.getY() + pandora.getEyeHeight(), pandora.getZ());
		if (killer != null && ServerPlayNetworking.canSend(killer, PandoraDeathS2CPayload.TYPE)) {
			ServerPlayNetworking.send(killer, payload);
		}
		if (ServerPlayNetworking.canSend(pandora, PandoraDeathS2CPayload.TYPE)) {
			ServerPlayNetworking.send(pandora, payload);
		}
	}

	private static ServerPlayer resolveKiller(DamageSource source, ServerPlayer pandora) {
		Entity attacker = source.getEntity();
		if (attacker instanceof ServerPlayer sp && sp != pandora) {
			return sp;
		}
		Entity direct = source.getDirectEntity();
		if (direct instanceof ServerPlayer sp && sp != pandora) {
			return sp;
		}
		return null;
	}

	/** A point ~1.4 blocks behind the given player, at their feet level. */
	private static Vec3 behindOf(ServerPlayer killer) {
		float yawRad = (float) Math.toRadians(killer.getYRot());
		double bx = killer.getX() + Math.sin(yawRad) * 1.4;
		double bz = killer.getZ() - Math.cos(yawRad) * 1.4;
		return new Vec3(bx, killer.getY(), bz);
	}
}
