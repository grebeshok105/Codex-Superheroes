package com.example.superheroes.effect;

import com.example.superheroes.ability.RaidenPlungingStrikeAbility;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * Ловит переход air→ground у Райден с активным «armed»-окном Plunging Strike.
 * При приземлении вызывает {@link RaidenPlungingStrikeAbility#onLanding(ServerPlayer)}.
 */
public final class RaidenPlungingLandingController {
	private static final Map<UUID, Boolean> PREV_ON_GROUND = new HashMap<>();

	private RaidenPlungingLandingController() {
	}

	public static void init() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				PREV_ON_GROUND.remove(handler.getPlayer().getUUID()));
	}

	private static void tick(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		boolean onGround = player.onGround();
		Boolean prev = PREV_ON_GROUND.put(player.getUUID(), onGround);
		if (!data.hasHero() || !RaidenHero.ID.equals(data.heroId())) return;

		if (prev != null && !prev && onGround) {
			RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
			long now = player.serverLevel().getGameTime();
			if (state.plungingArmedUntilTick() > now) {
				RaidenPlungingStrikeAbility.onLanding(player);
			}
		}
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		tick(player);
	}

}
