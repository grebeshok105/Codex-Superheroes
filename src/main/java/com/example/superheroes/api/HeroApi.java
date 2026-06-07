package com.example.superheroes.api;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.transform.HeroData;
import com.example.superheroes.transform.HeroTransformService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Stable entry point for hero-related operations. Addon mods should use this
 * facade instead of touching internal classes like {@code Heroes},
 * {@code HeroTransformService}, or {@code ModAttachments} directly.
 *
 * <p>This is a server-authoritative API: state-changing methods
 * ({@link #transform(ServerPlayer, ResourceLocation)},
 * {@link #untransform(ServerPlayer)}) accept {@link ServerPlayer} only.
 * Read-only methods accept {@link Player} so they can be called from either
 * side, but the returned data is only authoritative on the server.
 */
public final class HeroApi {
	private HeroApi() {
	}

	/**
	 * Register a custom hero implementation. Idempotent on identical IDs:
	 * registering twice with the same {@link Hero#getId()} replaces the
	 * previous registration. Call this from your mod's {@code onInitialize}.
	 *
	 * @param hero hero implementation
	 */
	public static void register(Hero hero) {
		Heroes.register(hero);
	}

	/**
	 * Look up a registered hero by its ID.
	 *
	 * @param id hero id, e.g. {@code superheroes:homelander} or
	 *           {@code yourmod:reinhard}
	 * @return registered hero, or {@code null} if no hero with this id is
	 *         registered
	 */
	@Nullable
	public static Hero get(@Nullable ResourceLocation id) {
		return Heroes.get(id);
	}

	/**
	 * Snapshot of all currently registered heroes, keyed by ID. The returned
	 * map is unmodifiable.
	 */
	public static Map<ResourceLocation, Hero> all() {
		return Heroes.all();
	}

	/**
	 * Whether the player is currently transformed into any hero.
	 */
	public static boolean hasHero(Player player) {
		return getCurrentHeroId(player).isPresent();
	}

	/**
	 * The ID of the player's current hero, or empty if not transformed.
	 *
	 * <p>Reads from a Fabric attachment that is synchronised across the
	 * client and server, so this can be called from both sides.
	 */
	public static Optional<ResourceLocation> getCurrentHeroId(Player player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.currentHero();
	}

	/**
	 * The {@link Hero} object the player is currently transformed into, or
	 * {@code null} if not transformed (or if the hero is unknown).
	 */
	@Nullable
	public static Hero getCurrentHero(Player player) {
		return getCurrentHeroId(player).map(Heroes::get).orElse(null);
	}

	/**
	 * The player's current energy. Returns {@code 0} if the player is not
	 * transformed.
	 */
	public static float getEnergy(Player player) {
		return player.getAttachedOrCreate(ModAttachments.HERO_DATA).energy();
	}

	/**
	 * The player's current mana. Returns {@code 0} if the player is not
	 * transformed.
	 */
	public static float getMana(Player player) {
		return player.getAttachedOrCreate(ModAttachments.HERO_DATA).mana();
	}

	/**
	 * Transform the player into the given hero. Fails (returns {@code false})
	 * if no hero with this ID is registered, if the player is currently in
	 * transform-cooldown, or if the player is already this hero.
	 *
	 * <p>Server-side only.
	 *
	 * @param player target player
	 * @param heroId hero id (e.g. {@code yourmod:reinhard})
	 * @return {@code true} if the transformation succeeded
	 */
	public static boolean transform(ServerPlayer player, ResourceLocation heroId) {
		return HeroTransformService.transform(player, heroId);
	}

	/**
	 * Untransform the player. Fails (returns {@code false}) if the player
	 * is not currently transformed, or is in transform-cooldown.
	 *
	 * <p>Server-side only.
	 *
	 * @return {@code true} if the player was transformed and is now reverted
	 */
	public static boolean untransform(ServerPlayer player) {
		return HeroTransformService.untransform(player);
	}

	/**
	 * Force-untransform the player, bypassing the transform cooldown. Use
	 * sparingly — most callers should use {@link #untransform(ServerPlayer)}.
	 *
	 * <p>Server-side only.
	 */
	public static boolean forceUntransform(ServerPlayer player) {
		return HeroTransformService.forceUntransform(player);
	}
}
