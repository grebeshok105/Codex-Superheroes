package com.example.superheroes.horde;

import net.minecraft.world.entity.EntityType;

import java.util.List;

/**
 * Описание одной волны: обычные мобы + опциональный босс.
 */
public record HordeWaveDefinition(
		int waveNumber,
		List<MobEntry> mobs,
		MobEntry boss
) {
	public record MobEntry(EntityType<?> type, int count) {
	}

	public int totalMobCount() {
		int sum = mobs.stream().mapToInt(MobEntry::count).sum();
		if (boss != null) sum += boss.count();
		return sum;
	}
}
