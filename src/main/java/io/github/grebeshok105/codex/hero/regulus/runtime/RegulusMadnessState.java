package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Regulus madness state — absolute game-tick deadlines, never durations.
 * {@code ritualUntilTick} is the open reading window of the Evangelion ritual
 * (0 = not reading); {@code madnessUntilTick} is when the 900-tick madness
 * collapses (0 = permanent/unbounded while {@code madness} is set).
 *
 * <p>Synced to the owner and every tracking player — the client reads the same
 * attachment off {@code mc.player} and derives progress from
 * {@code level.getGameTime()}, so a late-joining observer sees the same
 * deadlines instead of a latched wall-clock. The old {@code madness_sync} /
 * {@code madness_visual} payloads are gone — HUD visuals ride edge detectors on
 * this state. Bonus life is deliberately NOT a field here: its single
 * authoritative source is {@link RegulusBonusLife#ATTACHMENT}.
 */
public record RegulusMadnessState(
		boolean madness,
		long ritualUntilTick,
		long madnessUntilTick
) {
	public static final RegulusMadnessState EMPTY = new RegulusMadnessState(false, 0L, 0L);

	public static final StreamCodec<RegistryFriendlyByteBuf, RegulusMadnessState> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.BOOL, RegulusMadnessState::madness,
					ByteBufCodecs.VAR_LONG, RegulusMadnessState::ritualUntilTick,
					ByteBufCodecs.VAR_LONG, RegulusMadnessState::madnessUntilTick,
					RegulusMadnessState::new
			);

	/**
	 * The state attachment itself — created on this leaf so leaf code never imports the module
	 * root; {@code RegulusAttachments.REGULUS_MADNESS} re-exports it for module-external readers.
	 * NOT persistent and no copyOnDeath — death/leave respawns clean (the lifecycle hooks clear
	 * it anyway). The AttachmentRegistrar seam has no syncWith overload, so this stays a direct
	 * {@link AttachmentRegistry#create} call like {@code ModAttachments} had.
	 * Created eagerly at class-init — see {@code RegulusAttachments.init()}.
	 */
	public static final AttachmentType<RegulusMadnessState> ATTACHMENT =
			AttachmentRegistry.create(ModId.of("regulus_madness"), b -> b
					.initializer(() -> RegulusMadnessState.EMPTY)
					.syncWith(STREAM_CODEC, AttachmentSyncPredicate.all()));

	public RegulusMadnessState withMadness(boolean v) {
		return new RegulusMadnessState(v, ritualUntilTick, madnessUntilTick);
	}

	public RegulusMadnessState withRitual(long gameTime) {
		return new RegulusMadnessState(madness, gameTime, madnessUntilTick);
	}

	public RegulusMadnessState withMadnessUntil(long gameTime) {
		return new RegulusMadnessState(madness, ritualUntilTick, gameTime);
	}

	public boolean isReading(long gameTime) {
		return ritualUntilTick > gameTime;
	}
}
