package com.example.superheroes.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record DoomsdayProgress(
		int tier,
		Set<String> deathSources,
		int adaptations,
		double lastDeathX,
		double lastDeathY,
		double lastDeathZ,
		boolean pendingRelocate,
		long lastTierUpTick
) {
	public static final DoomsdayProgress EMPTY = new DoomsdayProgress(
			1, Set.of(), 0, 0.0, 0.0, 0.0, false, 0L);

	public static final Codec<DoomsdayProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("tier", 1).forGetter(DoomsdayProgress::tier),
			Codec.STRING.listOf().xmap(
					l -> (Set<String>) new HashSet<>(l),
					s -> List.copyOf(s)
			).optionalFieldOf("death_sources", Set.of()).forGetter(DoomsdayProgress::deathSources),
			Codec.INT.optionalFieldOf("adaptations", 0).forGetter(DoomsdayProgress::adaptations),
			Codec.DOUBLE.optionalFieldOf("ldx", 0.0).forGetter(DoomsdayProgress::lastDeathX),
			Codec.DOUBLE.optionalFieldOf("ldy", 0.0).forGetter(DoomsdayProgress::lastDeathY),
			Codec.DOUBLE.optionalFieldOf("ldz", 0.0).forGetter(DoomsdayProgress::lastDeathZ),
			Codec.BOOL.optionalFieldOf("pending_relocate", false).forGetter(DoomsdayProgress::pendingRelocate),
			Codec.LONG.optionalFieldOf("last_tier_up", 0L).forGetter(DoomsdayProgress::lastTierUpTick)
	).apply(instance, DoomsdayProgress::new));

	public DoomsdayProgress {
		deathSources = Set.copyOf(deathSources);
	}

	public Vec3 lastDeathPos() {
		return new Vec3(lastDeathX, lastDeathY, lastDeathZ);
	}

	public DoomsdayProgress withTier(int newTier) {
		return new DoomsdayProgress(newTier, deathSources, adaptations, lastDeathX, lastDeathY, lastDeathZ,
				pendingRelocate, lastTierUpTick);
	}

	public DoomsdayProgress withTierUp(long tick) {
		int newTier = Math.min(7, tier + 1);
		return new DoomsdayProgress(newTier, deathSources, adaptations + 1, lastDeathX, lastDeathY, lastDeathZ,
				pendingRelocate, tick);
	}

	public DoomsdayProgress withDeathSource(String source, Vec3 pos) {
		Set<String> next = new HashSet<>(deathSources);
		next.add(source);
		return new DoomsdayProgress(tier, next, adaptations, pos.x, pos.y, pos.z, true, lastTierUpTick);
	}

	public DoomsdayProgress withRelocateClear() {
		return new DoomsdayProgress(tier, deathSources, adaptations, lastDeathX, lastDeathY, lastDeathZ, false, lastTierUpTick);
	}

	public DoomsdayProgress reset() {
		return EMPTY;
	}
}
