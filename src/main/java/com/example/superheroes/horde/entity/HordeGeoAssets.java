package com.example.superheroes.horde.entity;

import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Связка имён GeckoLib-анимаций для тварей орды. Гео-модель, текстура и файл
 * анимаций берутся по короткому имени твари (id без префикса {@code horde_}),
 * а конкретные имена клипов idle/walk/attack различаются от мода к моду
 * (часть называется {@code animation.projectparasites.X.*}, часть просто
 * {@code animation.X.*}), поэтому держим их в явной таблице.
 */
public final class HordeGeoAssets {
	/** idle/walk/attack — любой из walk/attack может быть {@code null}. */
	public record Anims(String idle, String walk, String attack) {
	}

	private static final Map<String, Anims> BY_NAME = new HashMap<>();

	static {
		put("crawler", "animation.projectparasites.crawler.idle", "animation.projectparasites.crawler.walk", "animation.projectparasites.crawler.attack");
		put("lurker", "animation.projectparasites.lurker.idle", "animation.projectparasites.lurker.crawl", "animation.projectparasites.lurker.attack");
		put("spitter", "animation.projectparasites.spitter.idle", "animation.projectparasites.spitter.walk", "animation.projectparasites.spitter.spit");
		put("swooper", "animation.swooper.idle", "animation.swooper.fly", "animation.swooper.attack");
		put("stalker", "animation.projectparasites.stalker.idle", "animation.projectparasites.stalker.stalk", "animation.projectparasites.stalker.combo");
		put("infector", "animation.projectparasites.infector.idle", "animation.projectparasites.infector.move", "animation.projectparasites.infector.infect");
		put("parasitic_hound", "animation.parasitic_hound.idle", "animation.parasitic_hound.walk", "animation.parasitic_hound.attack");
		put("infected_zombie", "animation.infected_zombie.idle", "animation.infected_zombie.walk", "animation.infected_zombie.attack");
		put("infected_skeleton", "animation.infected_skeleton.idle", "animation.infected_skeleton.walk", "animation.infected_skeleton.attack");
		put("infected_spider", "animation.infected_spider.idle", "animation.infected_spider.walk", "animation.infected_spider.attack");
		put("infected_creeper", "animation.infected_creeper.idle", "animation.infected_creeper.walk", "animation.infected_creeper.attack");
		put("void_parasite", "animation.void_parasite.idle", "animation.void_parasite.walk", "animation.void_parasite.attack");
		put("hollow_villager", "animation.hollow_villager.idle", "animation.hollow_villager.walk", "animation.hollow_villager.attack");
		put("infected_cattle", "animation.infected_cattle.idle", "animation.infected_cattle.walk", "animation.infected_cattle.attack");
		put("broodmother", "animation.projectparasites.broodmother.idle", "animation.projectparasites.broodmother.walk", null);
		put("corrupted_golem", "animation.corrupted_golem.idle", "animation.corrupted_golem.walk", "animation.corrupted_golem.attack");
		put("hivemind", "animation.projectparasites.hivemind.idle", null, "animation.projectparasites.hivemind.slam");
		put("leviathan", "animation.projectparasites.leviathan.idle", "animation.projectparasites.leviathan.swim", "animation.projectparasites.leviathan.lunge");
	}

	private HordeGeoAssets() {
	}

	private static void put(String name, String idle, String walk, String attack) {
		BY_NAME.put(name, new Anims(idle, walk, attack));
	}

	/** Короткое имя гео-ассета: id типа без префикса {@code horde_}. */
	public static String geoName(EntityType<?> type) {
		String path = EntityType.getKey(type).getPath();
		return path.startsWith("horde_") ? path.substring("horde_".length()) : path;
	}

	public static Anims anims(String geoName) {
		return BY_NAME.get(geoName);
	}

	public static boolean has(String geoName) {
		return BY_NAME.containsKey(geoName);
	}
}
