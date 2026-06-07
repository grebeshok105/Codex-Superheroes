package com.example.superheroes.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record ReinhardState(
		int phase,
		float accumulatedDamage,
		boolean swordDrawn,
		boolean phoenixUsed,
		int wishesUsed,
		Optional<UUID> judgmentTarget,
		long judgmentExpireTick,
		List<String> recentDamageTypes,
		List<String> adaptedDamageTypes,
		long lastWishTick,
		long lastInstaRegenTick,
		long lastWorthyHitTick,
		float worthyAccumulatedDamage,
		long riposteExpireTick,
		int phoenixCount,
		boolean inSecondComing
) {
	public static final ReinhardState EMPTY = new ReinhardState(
			1, 0f, false, false, 0,
			Optional.empty(), 0L,
			List.of(), List.of(),
			0L, 0L, 0L, 0f, 0L, 0,
			false
	);

	public static final Codec<ReinhardState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("phase", 1).forGetter(ReinhardState::phase),
			Codec.FLOAT.optionalFieldOf("accumulated_damage", 0f).forGetter(ReinhardState::accumulatedDamage),
			Codec.BOOL.optionalFieldOf("sword_drawn", false).forGetter(ReinhardState::swordDrawn),
			Codec.BOOL.optionalFieldOf("phoenix_used", false).forGetter(ReinhardState::phoenixUsed),
			Codec.INT.optionalFieldOf("wishes_used", 0).forGetter(ReinhardState::wishesUsed),
			com.mojang.serialization.Codec.STRING.xmap(java.util.UUID::fromString, java.util.UUID::toString)
					.optionalFieldOf("judgment_target").forGetter(ReinhardState::judgmentTarget),
			Codec.LONG.optionalFieldOf("judgment_expire_tick", 0L).forGetter(ReinhardState::judgmentExpireTick),
			Codec.STRING.listOf().optionalFieldOf("recent_damage_types", List.of()).forGetter(ReinhardState::recentDamageTypes),
			Codec.STRING.listOf().optionalFieldOf("adapted_damage_types", List.of()).forGetter(ReinhardState::adaptedDamageTypes),
			Codec.LONG.optionalFieldOf("last_wish_tick", 0L).forGetter(ReinhardState::lastWishTick),
			Codec.LONG.optionalFieldOf("last_insta_regen_tick", 0L).forGetter(ReinhardState::lastInstaRegenTick),
			Codec.LONG.optionalFieldOf("last_worthy_hit_tick", 0L).forGetter(ReinhardState::lastWorthyHitTick),
			Codec.FLOAT.optionalFieldOf("worthy_accumulated_damage", 0f).forGetter(ReinhardState::worthyAccumulatedDamage),
			Codec.LONG.optionalFieldOf("riposte_expire_tick", 0L).forGetter(ReinhardState::riposteExpireTick),
			Codec.INT.optionalFieldOf("phoenix_count", 0).forGetter(ReinhardState::phoenixCount),
			Codec.BOOL.optionalFieldOf("in_second_coming", false).forGetter(ReinhardState::inSecondComing)
	).apply(instance, ReinhardState::new));

	public ReinhardState withPhase(int p) {
		return new ReinhardState(p, accumulatedDamage, swordDrawn, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withAccumulatedDamage(float v) {
		return new ReinhardState(phase, v, swordDrawn, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withSwordDrawn(boolean v) {
		return new ReinhardState(phase, accumulatedDamage, v, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withPhoenixUsed(boolean v) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, v, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withPhoenixCount(int v) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, v, inSecondComing);
	}

	public ReinhardState withWishesUsed(int v) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, phoenixUsed, v, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withJudgmentTarget(Optional<UUID> target, long expireTick) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, phoenixUsed, wishesUsed, target,
				expireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withInSecondComing(boolean v) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, v);
	}

	public ReinhardState withRecentDamageTypes(List<String> v) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, List.copyOf(v), adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withAdaptedDamageTypes(List<String> v) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, List.copyOf(v), lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withLastWishTick(long v) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, v, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withLastInstaRegenTick(long v) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, v,
				lastWorthyHitTick, worthyAccumulatedDamage, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withWorthy(long lastTick, float accum) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastTick, accum, riposteExpireTick, phoenixCount, inSecondComing);
	}

	public ReinhardState withRiposteExpireTick(long v) {
		return new ReinhardState(phase, accumulatedDamage, swordDrawn, phoenixUsed, wishesUsed, judgmentTarget,
				judgmentExpireTick, recentDamageTypes, adaptedDamageTypes, lastWishTick, lastInstaRegenTick,
				lastWorthyHitTick, worthyAccumulatedDamage, v, phoenixCount, inSecondComing);
	}
}
