package io.github.grebeshok105.codex.lifecycle;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.transform.HeroData;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Audit B12: hero passives are infinite {@link MobEffectInstance}s applied once at transform —
 * milk, {@code removeAllEffects} death-saves and targeted {@code removeEffect} calls wiped them
 * for good. The reconciler records what each hero's {@code applyPassives} actually declares and,
 * after any effect removal on a player, re-asserts the missing ones at end of tick, for that
 * player only. It never touches stronger live instances of the same effect: vanilla's
 * {@code MobEffectInstance.update} nesting keeps temporary buffs (or a nested passive) on top.
 */
public final class PassiveReconciler {
	private record Declared(ResourceLocation heroId, Map<Holder<MobEffect>, MobEffectInstance> effects) {
	}

	private record Capture(ServerPlayer player, Map<Holder<MobEffect>, MobEffectInstance> effects) {
	}

	/** Effect instances currently applying for the armed {@link #capture} scope. */
	private static final ThreadLocal<Capture> CAPTURING = new ThreadLocal<>();

	private static final Map<UUID, Declared> DECLARED = new ConcurrentHashMap<>();
	private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

	private PassiveReconciler() {
	}

	public static void init() {
		PlayerLifecycle.onServerStopped(server -> reset());
	}

	/**
	 * Runs {@code applyPassives} and records every infinite effect it adds as the player's
	 * declared passives (replacing the previous record). Call wherever hero passives are
	 * (re)applied — transform, join, respawn, Doomsday tier re-derives.
	 */
	public static void capture(ServerPlayer player, ResourceLocation heroId, Runnable applyPassives) {
		Capture capture = new Capture(player, new LinkedHashMap<>());
		CAPTURING.set(capture);
		try {
			applyPassives.run();
		} finally {
			CAPTURING.remove();
		}
		DECLARED.put(player.getUUID(), new Declared(heroId, Map.copyOf(capture.effects())));
	}

	public static void applyAndCapture(ServerPlayer player, Hero hero) {
		capture(player, hero.getId(), () -> hero.applyPassives(player));
	}

	/**
	 * Mixin hook for {@code LivingEntity#addEffect}: records the declared instance while a
	 * {@link #capture} scope is armed on this thread and the target is the captured player.
	 */
	public static void onAddEffect(LivingEntity entity, MobEffectInstance instance) {
		Capture capture = CAPTURING.get();
		if (capture == null || capture.player() != entity || !instance.isInfiniteDuration()) {
			return;
		}
		capture.effects().putIfAbsent(instance.getEffect(), new MobEffectInstance(instance));
	}

	/**
	 * Mixin hook for {@code LivingEntity#onEffectRemoved}: fires on every removal path
	 * (milk / {@code removeAllEffects}, targeted {@code removeEffect}, natural expiry), so one
	 * deferred reconcile per player covers them all.
	 */
	public static void onEffectRemoved(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			PENDING.add(player.getUUID());
		}
	}

	public static void clear(UUID playerId) {
		DECLARED.remove(playerId);
		PENDING.remove(playerId);
	}

	public static void reset() {
		DECLARED.clear();
		PENDING.clear();
	}

	public static void serverTick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		for (UUID id : PENDING) {
			PENDING.remove(id);
			Declared declared = DECLARED.get(id);
			if (declared == null) {
				continue;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {
				continue;
			}
			HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (!data.hasHero() || !declared.heroId().equals(data.heroId())) {
				// hero swapped or removed — the new applyAndCapture already replaced the record;
				// a stale record must never resurrect another hero's passives
				DECLARED.remove(id);
				continue;
			}
			for (Map.Entry<Holder<MobEffect>, MobEffectInstance> entry : declared.effects().entrySet()) {
				if (!player.hasEffect(entry.getKey())) {
					player.addEffect(new MobEffectInstance(entry.getValue()));
				}
			}
		}
	}
}
