package io.github.grebeshok105.codex.client.core.module;

import io.github.grebeshok105.codex.client.core.hud.AbilityDecoration;
import io.github.grebeshok105.codex.client.core.hud.HudLayer;
import io.github.grebeshok105.codex.client.core.hud.MovableHud;
import io.github.grebeshok105.codex.client.core.render.SkinProvider;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface HeroClientContext {
	<T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, ClientPlayNetworking.PlayPayloadHandler<T> handler);

	void hud(int order, ResourceLocation id, HudLayer layer);

	void movableHud(int order, ResourceLocation id, HudLayer layer, MovableHud movable);

	/**
	 * Registers a hero action key. {@code onPress} runs once per press, only while the local player is this module's
	 * hero. The mapping's name must stay the one already in players' options.txt.
	 */
	KeyMapping actionKey(KeyMapping mapping, Consumer<Minecraft> onPress);

	/** Registers the renderer of an entity type owned by this module's hero. */
	<T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> provider);

	/**
	 * Registers the {@link SkinProvider} that picks this module's hero skin. The provider must preserve the
	 * exact conditions its hero had in the pre-CL4 skin mixins — see the interface's javadoc for the list.
	 */
	void skin(SkinProvider provider);

	/**
	 * Queues a feature layer factory attached to every {@link PlayerRenderer}. Layer constructors take the
	 * renderer as {@code RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>}, so a
	 * constructor reference like {@code MyLayer::new} works directly.
	 */
	void playerLayer(Function<PlayerRenderer, RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> factory);

	/**
	 * Registers a predicate consulted by the sound engine for every played sound; the sound is muted while
	 * any registered predicate returns {@code true}.
	 */
	void soundFilter(Predicate<SoundInstance> mute);

	/**
	 * Registers an {@link AbilityDecoration} drawn around {@code abilityId}'s icon in the radial menu
	 * (a ready halo, a badge, …). The decoration itself decides per frame whether to draw.
	 */
	void abilityDecoration(ResourceLocation abilityId, AbilityDecoration decoration);
}
