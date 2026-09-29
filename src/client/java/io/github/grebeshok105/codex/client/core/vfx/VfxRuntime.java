package io.github.grebeshok105.codex.client.core.vfx;

import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.core.vfx.backend.FallbackVfxBackend;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParamsLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client Visual Core runtime: the registry of one-shot {@link VfxEffect} and
 * {@link VfxChannelEffect} factories plus the live-effect tables the payload
 * receivers drive. Budget, eviction and channel expiry live in the
 * package-private {@link VfxInstanceTable} / {@link VfxChannelTable} so JUnit
 * can exercise them without a Minecraft instance; this class is the wiring —
 * factory lookup, camera culling, thread-side dispatch.
 */
public final class VfxRuntime {
	/** Maximum simultaneously live effects; the oldest is evicted via {@code cancel()}. */
	public static final int MAX_ACTIVE_EFFECTS = 256;
	/** Spawn requests whose origin sits farther than this from the camera are dropped. */
	public static final double CULL_DISTANCE = 160.0;
	/** Ticks of channel silence after which the client releases the effect. */
	public static final int CHANNEL_TIMEOUT_TICKS = 10;

	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes-vfx");
	private static final Map<ResourceLocation, VfxEffectFactory> EFFECT_FACTORIES = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, VfxChannelFactory> CHANNEL_FACTORIES = new ConcurrentHashMap<>();
	private static final Set<ResourceLocation> UNKNOWN_LOGGED = ConcurrentHashMap.newKeySet();

	private static VfxInstanceTable instances;
	private static VfxChannelTable channels;

	static {
		ClientSessionState.register(VfxRuntime::reset);
		reset();
	}

	private VfxRuntime() {
	}

	/** Registers the render/tick/reload hooks; called once from the client bootstrap. */
	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
		WorldRenderEvents.AFTER_TRANSLUCENT.register(VfxRuntime::render);
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new VfxParamsLoader());
		HudRenderCallback.EVENT.register(FallbackVfxBackend::renderFlashHud);
	}

	public static void registerEffect(ResourceLocation id, VfxEffectFactory factory) {
		EFFECT_FACTORIES.put(id, factory);
	}

	public static void registerChannel(ResourceLocation id, VfxChannelFactory factory) {
		CHANNEL_FACTORIES.put(id, factory);
	}

	/**
	 * Spawns a one-shot effect. Returns {@code false} when the id has no
	 * registered factory (debug-logged once) or the origin is culled beyond
	 * {@link #CULL_DISTANCE} from the camera.
	 */
	public static boolean spawn(VfxSpawn spawn) {
		VfxEffectFactory factory = EFFECT_FACTORIES.get(spawn.effect());
		if (factory == null) {
			logUnknownOnce(spawn.effect());
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return false;
		}
		if (spawn.origin().distanceToSqr(client.gameRenderer.getMainCamera().getPosition())
				> CULL_DISTANCE * CULL_DISTANCE) {
			return false;
		}
		VfxEffect effect = factory.create(spawn);
		if (effect == null) {
			return false;
		}
		instances.add(effect);
		return true;
	}

	/** Routes a channel wire state to the channel table. */
	public static void channel(int entityId, ResourceLocation channel, byte state, Vec3 target) {
		channels.channel(entityId, channel, state, target);
	}

	public static void tick() {
		instances.tick();
		channels.tick();
	}

	public static void render(WorldRenderContext world) {
		if (instances.size() == 0) {
			return;
		}
		instances.render(new VfxRenderContext(world, world.matrixStack(), world.consumers(),
				world.camera(), world.tickCounter().getGameTimeDeltaPartialTick(true)));
	}

	public static int activeCount() {
		return instances.size();
	}

	/** Live channel effects — the debug HUD's "open channels" count. */
	public static int openChannelCount() {
		return channels.size();
	}

	/** Session reset (disconnect / world leave): drops every live effect and channel. */
	public static void reset() {
		instances = new VfxInstanceTable(MAX_ACTIVE_EFFECTS);
		channels = new VfxChannelTable(instances, VfxRuntime::openChannel, CHANNEL_TIMEOUT_TICKS);
		instances.setRemovalListener(channels::forget);
		UNKNOWN_LOGGED.clear();
	}

	@Nullable
	private static VfxChannelEffect openChannel(int entityId, ResourceLocation channelId, Vec3 target) {
		VfxChannelFactory factory = CHANNEL_FACTORIES.get(channelId);
		if (factory == null) {
			logUnknownOnce(channelId);
			return null;
		}
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return null;
		}
		Entity source = level.getEntity(entityId);
		if (source == null) {
			return null;
		}
		return factory.open(source, target, VfxParamsLoader.get(channelId));
	}

	private static void logUnknownOnce(ResourceLocation id) {
		if (UNKNOWN_LOGGED.add(id)) {
			LOGGER.debug("ignoring vfx request for unregistered id {}", id);
		}
	}
}
