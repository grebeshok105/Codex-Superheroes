package com.example.superheroes.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
