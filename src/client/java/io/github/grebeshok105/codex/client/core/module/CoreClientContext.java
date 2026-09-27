package io.github.grebeshok105.codex.client.core.module;

import io.github.grebeshok105.codex.client.core.FovModifier;
import io.github.grebeshok105.codex.client.core.FovModifiers;
import io.github.grebeshok105.codex.client.core.HudGlitchSource;
import io.github.grebeshok105.codex.client.core.HudJitter;
import io.github.grebeshok105.codex.client.core.audio.ClientSoundFilters;
import io.github.grebeshok105.codex.client.core.hud.AbilityDecoration;
import io.github.grebeshok105.codex.client.core.hud.AbilityDecorations;
import io.github.grebeshok105.codex.client.core.hud.HudLayer;
import io.github.grebeshok105.codex.client.core.hud.HudLayers;
import io.github.grebeshok105.codex.client.core.hud.MovableHud;
import io.github.grebeshok105.codex.client.core.input.HeroActionKeys;
import io.github.grebeshok105.codex.client.core.render.PlayerLayers;
import io.github.grebeshok105.codex.client.core.render.SkinProvider;
import io.github.grebeshok105.codex.client.core.render.SkinResolver;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
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

/** Default {@link HeroClientContext}: delegates to the real registries. One instance per module — {@link #heroId} scopes action-key filtering. */
public final class CoreClientContext implements HeroClientContext {
	private final ResourceLocation heroId;

	public CoreClientContext(ResourceLocation heroId) {
		this.heroId = heroId;
	}

	@Override
	public <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, ClientPlayNetworking.PlayPayloadHandler<T> handler) {
		ClientPlayNetworking.registerGlobalReceiver(type, handler);
	}

	@Override
	public void hud(int order, ResourceLocation id, HudLayer layer) {
		HudLayers.register(order, id, layer);
	}

	@Override
	public void movableHud(int order, ResourceLocation id, HudLayer layer, MovableHud movable) {
		HudLayers.registerMovable(order, id, layer, movable);
	}

	@Override
	public KeyMapping actionKey(KeyMapping mapping, Consumer<Minecraft> onPress) {
		return HeroActionKeys.register(heroId, mapping, onPress);
	}

	@Override
	public <T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> provider) {
		EntityRendererRegistry.register(type, provider);
	}

	@Override
	public <T extends ParticleOptions> void particle(ParticleType<T> type, ParticleFactoryRegistry.PendingParticleFactory<T> factory) {
		ParticleFactoryRegistry.getInstance().register(type, factory);
	}

	@Override
	public void skin(SkinProvider provider) {
		SkinResolver.register(heroId, provider);
	}

	@Override
	public void playerLayer(Function<PlayerRenderer, RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> factory) {
		PlayerLayers.register(factory);
	}

	@Override
	public void soundFilter(Predicate<SoundInstance> mute) {
		ClientSoundFilters.register(mute);
	}

	@Override
	public void abilityDecoration(ResourceLocation abilityId, AbilityDecoration decoration) {
		AbilityDecorations.register(abilityId, decoration);
	}

	@Override
	public void fovModifier(FovModifier modifier) {
		FovModifiers.register(modifier);
	}

	@Override
	public void hudGlitchSource(HudGlitchSource source) {
		HudJitter.register(source);
	}

	@Override
	public void clientTick(Consumer<Minecraft> hook) {
		ClientTickEvents.END_CLIENT_TICK.register(hook::accept);
	}
}
