package io.github.grebeshok105.codex.hero.raiden.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.model.HeroData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * Ловит переход air→ground у Райден с активным «armed»-окном Plunging Strike.
 * При приземлении вызывает {@link #onLanding(ServerPlayer)}.
 *
 * <p>NOTE: dead-but-pinned — nothing ever writes {@code plungingArmedUntilTick}, so the
 * landing branch never fires today. Kept wired exactly as before; the only change is the
 * static HashMap becoming an {@link OwnedSessionMap} that self-clears on leave (the former
 * raw DISCONNECT hook did the same removal).
 */
public final class RaidenPlungingLandingController {
	private static final ResourceLocation RAIDEN_ID = ModId.of("raiden_shogun");

	private static final OwnedSessionMap<UUID, Boolean> PREV_ON_GROUND =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE));

	private RaidenPlungingLandingController() {
	}

	private static void tick(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		boolean onGround = player.onGround();
		Boolean prev = PREV_ON_GROUND.get(player.getUUID());
		PREV_ON_GROUND.put(player.getUUID(), player.getUUID(), onGround);
		if (!data.hasHero() || !RAIDEN_ID.equals(data.heroId())) return;

		if (prev != null && !prev && onGround) {
			RaidenState state = player.getAttachedOrCreate(RaidenState.ATTACHMENT);
			long now = player.serverLevel().getGameTime();
			if (state.plungingArmedUntilTick() > now) {
				onLanding(player);
			}
		}
	}

	/** Landing action for an armed plunging strike — lives here so runtime does not
	 *  import back into the ability leaf. Still a no-op: HeavensStrikeController's
	 *  windup-based impact replaced it (kept dead-but-pinned as before the move). */
	private static void onLanding(ServerPlayer player) {
		// no-op
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		tick(player);
	}

}
