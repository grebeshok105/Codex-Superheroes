package io.github.grebeshok105.codex.client.core.render;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.hero.Heroes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Hero-agnostic skin resolution for the skin mixins. A client module registers its hero's
 * {@link SkinProvider} via {@code HeroClientContext.skin(provider)}; the mixins call
 * {@link #resolve(AbstractClientPlayer)} and apply the result.
 *
 * <p>Resolution chain for every texture: the provider's method → {@link Hero#getSkinTexture()}
 * → {@code null} (the call site's neutral default then applies). {@code null} from {@link #resolve}
 * itself means the player has no hero, so vanilla rendering stays untouched.
 */
public final class SkinResolver {
	private static final Map<ResourceLocation, SkinProvider> PROVIDERS = new HashMap<>();

	private SkinResolver() {
	}

	/** Called from client-module bootstrap only; one provider per hero id. */
	public static void register(ResourceLocation heroId, SkinProvider provider) {
		if (PROVIDERS.put(heroId, provider) != null) {
			throw new IllegalStateException("Duplicate SkinProvider for hero " + heroId);
		}
	}

	/**
	 * Resolves the hero view of {@code player}, or {@code null} when the player has no hero.
	 * The hero id comes from the same source the pre-CL4 mixins used: {@code ClientHeroState}
	 * for the local player, the synced {@code ModAttachments.PUBLIC_HERO} attachment for everyone else.
	 */
	@Nullable
	public static ResolvedSkin resolve(AbstractClientPlayer player) {
		ResourceLocation heroId = heroIdFor(player);
		if (heroId == null) {
			return null;
		}
		Hero hero = Heroes.get(heroId);
		ResourceLocation fallback = hero != null ? hero.getSkinTexture() : null;
		SkinProvider provider = PROVIDERS.get(heroId);
		ResourceLocation skin = provider != null ? provider.skin(player, heroId) : null;
		ResourceLocation handSkin = provider != null ? provider.handSkin(player, heroId) : null;
		Boolean slimModel = provider != null ? provider.slimModel(player, heroId) : null;
		return new ResolvedSkin(
				heroId,
				skin != null ? skin : fallback,
				handSkin != null ? handSkin : fallback,
				slimModel);
	}

	@Nullable
	private static ResourceLocation heroIdFor(AbstractClientPlayer player) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
			return ClientHeroState.heroId();
		}
		return player.getAttached(ModAttachments.PUBLIC_HERO);
	}

	/**
	 * A resolved hero view of a player. Textures may be {@code null} when neither the provider
	 * nor the hero supplied one — the call site then keeps its own default (default player skin
	 * for {@code getSkin}, the original texture for {@code renderHand}).
	 */
	public record ResolvedSkin(
			ResourceLocation heroId,
			@Nullable ResourceLocation texture,
			@Nullable ResourceLocation handTexture,
			@Nullable Boolean slimModel) {
	}
}
