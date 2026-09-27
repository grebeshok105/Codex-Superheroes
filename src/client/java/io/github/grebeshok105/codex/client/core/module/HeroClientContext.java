package io.github.grebeshok105.codex.client.core.module;

import io.github.grebeshok105.codex.client.core.FovModifier;
import io.github.grebeshok105.codex.client.core.HudGlitchSource;
import io.github.grebeshok105.codex.client.core.hud.AbilityDecoration;
import io.github.grebeshok105.codex.client.core.hud.HudLayer;
import io.github.grebeshok105.codex.client.core.hud.MovableHud;
import io.github.grebeshok105.codex.client.core.render.SkinProvider;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
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

	/** Registers the client factory of a particle type owned by this module's hero. */
	<T extends ParticleOptions> void particle(ParticleType<T> type, ParticleFactoryRegistry.PendingParticleFactory<T> factory);

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
	 * A decoration that also answers {@code true} from {@link AbilityDecoration#masksIdentity()} makes
	 * the ability panel hide that ability's identity — masked name, description, icon, cooldown —
	 * behind a "?" placeholder.
	 */
	void abilityDecoration(ResourceLocation abilityId, AbilityDecoration decoration);

	/**
	 * Registers an {@link FovModifier} for this hero (zoom channels, cinematic reads, …);
	 * the {@code GameRenderer#getFov} mixin folds every registered modifier into the fov result.
	 */
	void fovModifier(FovModifier modifier);

	/**
	 * Registers a {@link HudGlitchSource} owned by this hero (pose jitter, ghost double-render,
	 * colour bleed, text obfuscation); {@link io.github.grebeshok105.codex.client.core.HudJitter}
	 * serves it to shared HUD and vanilla-UI code while active.
	 */
	void hudGlitchSource(HudGlitchSource source);

	/** Registers a per-client-tick hook (END_CLIENT_TICK). */
	void clientTick(Consumer<Minecraft> hook);
}
