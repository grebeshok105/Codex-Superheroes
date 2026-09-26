package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.ability.RaidenPlungingStrikeAbility;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.RaidenHero;
import io.github.grebeshok105.codex.core.transform.HeroData;
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

	public static void register(HeroModuleContext ctx) {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				PREV_ON_GROUND.remove(handler.getPlayer().getUUID()));
	}

	private static void tick(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
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
