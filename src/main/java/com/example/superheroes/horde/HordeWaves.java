package com.example.superheroes.horde;

import com.example.superheroes.horde.entity.HordeEntities;
import com.example.superheroes.horde.HordeWaveDefinition.MobEntry;

import java.util.List;

/**
 * 10 волн орды, от лёгких к невыносимым.
 */
public final class HordeWaves {

	public static final List<HordeWaveDefinition> WAVES = List.of(
			// Wave 1: Вступление — мелкие разведчики
			new HordeWaveDefinition(1, List.of(
					new MobEntry(HordeEntities.CRAWLER, 8),
					new MobEntry(HordeEntities.PARASITIC_HOUND, 5),
					new MobEntry(HordeEntities.HOLLOW_VILLAGER, 4)
			), new MobEntry(HordeEntities.LURKER, 1)), // mini-boss: усиленный Lurker

			// Wave 2: Заражённые — знакомые лица, но мерзкие
			new HordeWaveDefinition(2, List.of(
					new MobEntry(HordeEntities.INFECTED_ZOMBIE, 6),
					new MobEntry(HordeEntities.INFECTED_SKELETON, 5),
					new MobEntry(HordeEntities.INFECTED_SPIDER, 4),
					new MobEntry(HordeEntities.CRAWLER, 4),
					new MobEntry(HordeEntities.HOLLOW_VILLAGER, 3)
			), new MobEntry(HordeEntities.BROODMOTHER, 1)),

			// Wave 3: Воздушный налёт + скрытные
			new HordeWaveDefinition(3, List.of(
					new MobEntry(HordeEntities.SWOOPER, 6),
					new MobEntry(HordeEntities.STALKER, 5),
					new MobEntry(HordeEntities.SPITTER, 4),
					new MobEntry(HordeEntities.CRAWLER, 5),
					new MobEntry(HordeEntities.PARASITIC_HOUND, 3)
			), null), // no boss

			// Wave 4: Инфекция — отравители
			new HordeWaveDefinition(4, List.of(
					new MobEntry(HordeEntities.INFECTOR, 5),
					new MobEntry(HordeEntities.SPITTER, 4),
					new MobEntry(HordeEntities.INFECTED_CREEPER, 4),
					new MobEntry(HordeEntities.VOID_PARASITE, 3),
					new MobEntry(HordeEntities.INFECTED_ZOMBIE, 4),
					new MobEntry(HordeEntities.INFECTED_CATTLE, 3)
			), new MobEntry(HordeEntities.CORRUPTED_GOLEM, 1)),

			// Wave 5: Стена — танки + снайперы
			new HordeWaveDefinition(5, List.of(
					new MobEntry(HordeEntities.CORRUPTED_GOLEM, 2),
					new MobEntry(HordeEntities.INFECTED_CATTLE, 5),
					new MobEntry(HordeEntities.LURKER, 5),
					new MobEntry(HordeEntities.INFECTED_SKELETON, 6),
					new MobEntry(HordeEntities.SPITTER, 4),
					new MobEntry(HordeEntities.INFECTED_ZOMBIE, 4)
			), new MobEntry(HordeEntities.HIVEMIND, 1)),

			// Wave 6: Хаос — всё смешано
			new HordeWaveDefinition(6, List.of(
					new MobEntry(HordeEntities.STALKER, 4),
					new MobEntry(HordeEntities.SWOOPER, 4),
					new MobEntry(HordeEntities.INFECTED_CREEPER, 5),
					new MobEntry(HordeEntities.VOID_PARASITE, 4),
					new MobEntry(HordeEntities.PARASITIC_HOUND, 4),
					new MobEntry(HordeEntities.INFECTOR, 3)
			), new MobEntry(HordeEntities.BROODMOTHER, 1)),

			// Wave 7: Элитный штурм
			new HordeWaveDefinition(7, List.of(
					new MobEntry(HordeEntities.VOID_PARASITE, 5),
					new MobEntry(HordeEntities.STALKER, 5),
					new MobEntry(HordeEntities.CORRUPTED_GOLEM, 2),
					new MobEntry(HordeEntities.SWOOPER, 5),
					new MobEntry(HordeEntities.INFECTED_CREEPER, 4),
					new MobEntry(HordeEntities.SPITTER, 4),
					new MobEntry(HordeEntities.LURKER, 3)
			), null),

			// Wave 8: Осада — боссы + масса
			new HordeWaveDefinition(8, List.of(
					new MobEntry(HordeEntities.BROODMOTHER, 2),
					new MobEntry(HordeEntities.CORRUPTED_GOLEM, 2),
					new MobEntry(HordeEntities.CRAWLER, 8),
					new MobEntry(HordeEntities.INFECTED_ZOMBIE, 5),
					new MobEntry(HordeEntities.INFECTED_SKELETON, 5),
					new MobEntry(HordeEntities.PARASITIC_HOUND, 5),
					new MobEntry(HordeEntities.VOID_PARASITE, 3)
			), new MobEntry(HordeEntities.LEVIATHAN, 1)),

			// Wave 9: Перед бурей — вообще дохрена
			new HordeWaveDefinition(9, List.of(
					new MobEntry(HordeEntities.HIVEMIND, 2),
					new MobEntry(HordeEntities.LEVIATHAN, 1),
					new MobEntry(HordeEntities.VOID_PARASITE, 5),
					new MobEntry(HordeEntities.STALKER, 5),
					new MobEntry(HordeEntities.SWOOPER, 5),
					new MobEntry(HordeEntities.INFECTED_CREEPER, 4),
					new MobEntry(HordeEntities.CORRUPTED_GOLEM, 2),
					new MobEntry(HordeEntities.INFECTOR, 4),
					new MobEntry(HordeEntities.CRAWLER, 6),
					new MobEntry(HordeEntities.INFECTED_CATTLE, 4)
			), null),

			// Wave 10: ФИНАЛ — Заражённый Хоумлендер + свита
			new HordeWaveDefinition(10, List.of(
					new MobEntry(HordeEntities.VOID_PARASITE, 6),
					new MobEntry(HordeEntities.STALKER, 4),
					new MobEntry(HordeEntities.BROODMOTHER, 2),
					new MobEntry(HordeEntities.CORRUPTED_GOLEM, 2),
					new MobEntry(HordeEntities.HIVEMIND, 1),
					new MobEntry(HordeEntities.SWOOPER, 4),
					new MobEntry(HordeEntities.INFECTED_CREEPER, 3),
					new MobEntry(HordeEntities.PARASITIC_HOUND, 5)
			), new MobEntry(HordeEntities.INFECTED_HOMELANDER, 1))
	);

	private HordeWaves() {
	}
}
