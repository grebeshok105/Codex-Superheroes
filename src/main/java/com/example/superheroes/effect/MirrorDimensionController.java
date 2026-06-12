package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.DoctorStrangeHero;
import com.example.superheroes.network.MirrorDimensionS2CPayload;
import com.example.superheroes.network.MirrorDimensionStatusC2SPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side state of Doctor Strange's Mirror Dimension: which caster warps
 * which victim. Sends ON/OFF/KEEPALIVE payloads to the victim's client and
 * relays the victim's status answers back to the caster.
 */
public final class MirrorDimensionController {
	private static final int KEEPALIVE_INTERVAL_TICKS = 20;

	private static final Map<UUID, UUID> CASTER_TO_VICTIM = new HashMap<>();
	private static final Map<UUID, UUID> VICTIM_TO_CASTER = new HashMap<>();
	private static int clock;

	private MirrorDimensionController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(MirrorDimensionController::tick);
	}

	/** @return false when the victim is already trapped by another Strange. */
	public static boolean start(ServerPlayer caster, ServerPlayer victim, int mode, int scale) {
		UUID existingCaster = VICTIM_TO_CASTER.get(victim.getUUID());
		if (existingCaster != null && !existingCaster.equals(caster.getUUID())) {
			caster.displayClientMessage(
					Component.translatable("ability.superheroes.mirror_dimension.busy").withStyle(ChatFormatting.RED), true);
			return false;
		}
		stop(caster, false);
		CASTER_TO_VICTIM.put(caster.getUUID(), victim.getUUID());
		VICTIM_TO_CASTER.put(victim.getUUID(), caster.getUUID());
		ServerPlayNetworking.send(victim, new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_ON, mode, scale));
		return true;
	}

	/** Stops the caster's active session (if any) and tells the victim to restore shaders. */
	public static void stop(ServerPlayer caster, boolean notifyCaster) {
		UUID victimId = CASTER_TO_VICTIM.remove(caster.getUUID());
		if (victimId == null) {
			return;
		}
		VICTIM_TO_CASTER.remove(victimId);
		ServerPlayer victim = caster.server.getPlayerList().getPlayer(victimId);
		if (victim != null) {
			ServerPlayNetworking.send(victim, new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_OFF, 0, 0));
		}
		if (notifyCaster) {
			caster.displayClientMessage(
					Component.translatable("ability.superheroes.mirror_dimension.released").withStyle(ChatFormatting.GRAY), true);
		}
	}

	public static void handleStatus(ServerPlayer victim, int status) {
		UUID casterId = VICTIM_TO_CASTER.get(victim.getUUID());
		ServerPlayer caster = casterId == null ? null : victim.server.getPlayerList().getPlayer(casterId);
		String key = switch (status) {
			case MirrorDimensionStatusC2SPayload.OK_APPLIED -> "ability.superheroes.mirror_dimension.applied";
			case MirrorDimensionStatusC2SPayload.NO_IRIS -> "ability.superheroes.mirror_dimension.no_iris";
			case MirrorDimensionStatusC2SPayload.NO_PACK -> "ability.superheroes.mirror_dimension.no_pack";
			case MirrorDimensionStatusC2SPayload.IRIS_API_FAIL -> "ability.superheroes.mirror_dimension.iris_fail";
			default -> null;
		};
		if (caster != null && key != null) {
			ChatFormatting color = status == MirrorDimensionStatusC2SPayload.OK_APPLIED
					? ChatFormatting.GREEN : ChatFormatting.RED;
			caster.displayClientMessage(
					Component.translatable(key, victim.getName()).withStyle(color), false);
		}
		boolean failed = status == MirrorDimensionStatusC2SPayload.NO_IRIS
				|| status == MirrorDimensionStatusC2SPayload.NO_PACK
				|| status == MirrorDimensionStatusC2SPayload.IRIS_API_FAIL;
		if (failed && caster != null) {
			// Don't burn the caster's mana on an effect the victim can't render.
			AbilityRouter.deactivate(caster, AbilityIds.MIRROR_DIMENSION);
		}
	}

	private static void tick(MinecraftServer server) {
		if (CASTER_TO_VICTIM.isEmpty()) {
			clock = 0;
			return;
		}
		clock++;
		boolean keepalive = clock % KEEPALIVE_INTERVAL_TICKS == 0;
		Iterator<Map.Entry<UUID, UUID>> it = CASTER_TO_VICTIM.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, UUID> entry = it.next();
			ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
			ServerPlayer victim = server.getPlayerList().getPlayer(entry.getValue());
			if (!sessionValid(caster, victim)) {
				it.remove();
				VICTIM_TO_CASTER.remove(entry.getValue());
				if (victim != null) {
					ServerPlayNetworking.send(victim,
							new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_OFF, 0, 0));
				}
				if (caster != null) {
					AbilityRouter.deactivate(caster, AbilityIds.MIRROR_DIMENSION);
				}
				continue;
			}
			if (keepalive) {
				ServerPlayNetworking.send(victim,
						new MirrorDimensionS2CPayload(MirrorDimensionS2CPayload.ACTION_KEEPALIVE, 0, 0));
			}
		}
	}

	private static boolean sessionValid(ServerPlayer caster, ServerPlayer victim) {
		if (caster == null || caster.isRemoved() || caster.isDeadOrDying()) {
			return false;
		}
		if (victim == null || victim.isRemoved() || victim.isDeadOrDying() || victim.isSpectator()) {
			return false;
		}
		HeroData data = caster.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero() || !DoctorStrangeHero.ID.equals(data.heroId())) {
			return false;
		}
		return data.isActive(AbilityIds.MIRROR_DIMENSION);
	}
}
