package io.github.grebeshok105.codex.hero.regulus.runtime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public record RegulusMadnessState(
		boolean madness,
		boolean bonusLifeAvailable,
		long manaRegenLockUntilTick,
		long readingUntilTick
) {
	public static final RegulusMadnessState EMPTY = new RegulusMadnessState(false, false, 0L, 0L);

	public static final Codec<RegulusMadnessState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("madness", false).forGetter(RegulusMadnessState::madness),
			Codec.BOOL.optionalFieldOf("bonus_life", false).forGetter(RegulusMadnessState::bonusLifeAvailable),
			Codec.LONG.optionalFieldOf("mana_lock_until_tick", 0L).forGetter(RegulusMadnessState::manaRegenLockUntilTick),
			Codec.LONG.optionalFieldOf("reading_until_tick", 0L).forGetter(RegulusMadnessState::readingUntilTick)
	).apply(i, RegulusMadnessState::new));

	/**
	 * The state attachment itself — created on this leaf so leaf code never imports the module
	 * root; {@code RegulusAttachments.REGULUS_MADNESS} re-exports it for module-external readers.
	 * Byte-identical to the former {@code ModAttachments.REGULUS_MADNESS} registration
	 * (id {@code superheroes:regulus_madness}, initializer, deliberately NOT persistent and
	 * without copyOnDeath).
	 * The AttachmentRegistrar seam has no transient-with-initializer overload, so this stays
	 * a direct {@link AttachmentRegistry#create} call like {@code ModAttachments} had.
	 * Created eagerly at class-init — see {@code RegulusAttachments.init()}.
	 */
	public static final AttachmentType<RegulusMadnessState> ATTACHMENT =
			AttachmentRegistry.create(ModId.of("regulus_madness"), b -> b
					.initializer(() -> RegulusMadnessState.EMPTY));

	public RegulusMadnessState withMadness(boolean v) {
		return new RegulusMadnessState(v, bonusLifeAvailable, manaRegenLockUntilTick, readingUntilTick);
	}

	public RegulusMadnessState withBonusLife(boolean v) {
		return new RegulusMadnessState(madness, v, manaRegenLockUntilTick, readingUntilTick);
	}

	public RegulusMadnessState withManaLock(long gameTime) {
		return new RegulusMadnessState(madness, bonusLifeAvailable, gameTime, readingUntilTick);
	}

	public RegulusMadnessState withReading(long gameTime) {
		return new RegulusMadnessState(madness, bonusLifeAvailable, manaRegenLockUntilTick, gameTime);
	}

	public boolean isReading(long gameTime) {
		return readingUntilTick > gameTime;
	}

	public boolean isManaRegenLocked(long gameTime) {
		return manaRegenLockUntilTick > gameTime;
	}
}
