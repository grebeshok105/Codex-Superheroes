package com.example.superheroes.api;

import com.example.superheroes.ability.Ability;
import com.example.superheroes.ability.AbilityRegistry;
import com.example.superheroes.resource.ResourceController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Stable entry point for ability-related operations. Addon mods should use
 * this facade instead of touching internal classes like {@code AbilityRegistry}
 * or {@code ResourceController} directly.
 */
public final class AbilityApi {
	private AbilityApi() {
	}

	/**
	 * Register a custom ability. Call this from your mod's {@code onInitialize}.
	 *
	 * <p>Note: registering an ability is not enough to make it available — the
	 * ability also needs to be referenced from a hero's
	 * {@link com.example.superheroes.hero.Hero#getAbilities()} list.
	 */
	public static void register(Ability ability) {
		AbilityRegistry.register(ability);
	}

	/**
	 * Look up a registered ability by its ID.
	 */
	@Nullable
	public static Ability get(@Nullable ResourceLocation id) {
		return AbilityRegistry.get(id);
	}

	/**
	 * Snapshot of all currently registered abilities, keyed by ID.
	 * Unmodifiable.
	 */
	public static Map<ResourceLocation, Ability> all() {
		return AbilityRegistry.all();
	}

	/**
	 * Try to consume the given amount of resource from the player for an
	 * ability activation. Routes through the player's {@code Energy/Mana}
	 * binding for that ability, with automatic fallback to the other resource
	 * if the primary is insufficient.
	 *
	 * <p>Server-side only.
	 *
	 * @param player    target player
	 * @param abilityId id of the ability the cost is being charged to
	 * @param amount    cost (must be positive)
	 * @return {@code true} if the cost was successfully deducted; {@code false}
	 *         if the player has neither enough energy nor enough mana, in
	 *         which case nothing was deducted
	 */
	public static boolean tryConsume(ServerPlayer player, ResourceLocation abilityId, float amount) {
		return ResourceController.tryConsume(player, abilityId, amount);
	}
}
