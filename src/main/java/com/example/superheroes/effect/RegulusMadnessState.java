package com.example.superheroes.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RegulusMadnessState(
		boolean madness,
		boolean bonusLifeAvailable,
		long manaRegenLockUntilMs,
		long readingUntilMs
) {
	public static final RegulusMadnessState EMPTY = new RegulusMadnessState(false, false, 0L, 0L);

	public static final Codec<RegulusMadnessState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("madness", false).forGetter(RegulusMadnessState::madness),
			Codec.BOOL.optionalFieldOf("bonus_life", false).forGetter(RegulusMadnessState::bonusLifeAvailable),
			Codec.LONG.optionalFieldOf("mana_lock_until", 0L).forGetter(RegulusMadnessState::manaRegenLockUntilMs),
			Codec.LONG.optionalFieldOf("reading_until", 0L).forGetter(RegulusMadnessState::readingUntilMs)
	).apply(i, RegulusMadnessState::new));

	public RegulusMadnessState withMadness(boolean v) {
		return new RegulusMadnessState(v, bonusLifeAvailable, manaRegenLockUntilMs, readingUntilMs);
	}

	public RegulusMadnessState withBonusLife(boolean v) {
		return new RegulusMadnessState(madness, v, manaRegenLockUntilMs, readingUntilMs);
	}

	public RegulusMadnessState withManaLock(long ms) {
		return new RegulusMadnessState(madness, bonusLifeAvailable, ms, readingUntilMs);
	}

	public RegulusMadnessState withReading(long ms) {
		return new RegulusMadnessState(madness, bonusLifeAvailable, manaRegenLockUntilMs, ms);
	}

	public boolean isReading() {
		return readingUntilMs > System.currentTimeMillis();
	}

	public boolean isManaRegenLocked() {
		return manaRegenLockUntilMs > System.currentTimeMillis();
	}
}
