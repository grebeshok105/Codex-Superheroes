package io.github.grebeshok105.codex.hero.doomsday.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DoomsdayEffectAdaptationController {
	private static final long REALTIME_THRESHOLD_TICKS = 200L;
	private static final long IDLE_RESET_TICKS = 300L;

	private static final Set<Holder<MobEffect>> TRACKED = new HashSet<>();
	static {
		TRACKED.add(MobEffects.WEAKNESS);
		TRACKED.add(MobEffects.MOVEMENT_SLOWDOWN);
		TRACKED.add(MobEffects.POISON);
		TRACKED.add(MobEffects.WITHER);
		TRACKED.add(MobEffects.HUNGER);
		TRACKED.add(MobEffects.CONFUSION);
		TRACKED.add(MobEffects.BLINDNESS);
	}

	private static final ResourceLocation DOOMSDAY_ID = ModId.of("doomsday");

	private static final OwnedSessionMap<UUID, Set<Holder<MobEffect>>> ADAPTED =
			OwnedSessionMap.create(LifecycleRegistrar.global(), Set.of(ClearOn.HERO_CLEAR));
	private static final OwnedSessionMap<UUID, Map<Holder<MobEffect>, Long>> FIRST_HIT_TICK =
			OwnedSessionMap.create(LifecycleRegistrar.global(), Set.of(ClearOn.HERO_CLEAR));

	private DoomsdayEffectAdaptationController() {
	}

	public static boolean isTracked(Holder<MobEffect> effect) {
		return TRACKED.contains(effect);
	}

	public static boolean hasAdapted(ServerPlayer player, Holder<MobEffect> effect) {
		Set<Holder<MobEffect>> adapted = ADAPTED.get(player.getUUID());
		return adapted != null && adapted.contains(effect);
	}

	/** {@code MobEffectGates} entry — fast-path skips untracked effects, then defers to {@link #onApply}. */
	public static boolean allow(ServerPlayer player, MobEffectInstance instance) {
		if (!isTracked(instance.getEffect())) return true;
		return onApply(player, instance);
	}

	/**
	 * Вызывается при applyEffect mixin-ом.
	 * @return true — эффект разрешить (накатить); false — отменить (адаптация).
	 */
	public static boolean onApply(ServerPlayer player, MobEffectInstance instance) {
		if (!isDoomsday(player)) return true;
		Holder<MobEffect> effect = instance.getEffect();
		if (!TRACKED.contains(effect)) return true;
		Set<Holder<MobEffect>> adapted = adapted(player.getUUID());
		if (adapted.contains(effect)) {
			return false;
		}
		long now = player.serverLevel().getGameTime();
		Map<Holder<MobEffect>, Long> firstHit = firstHit(player.getUUID());
		Long firstTs = firstHit.get(effect);
		if (firstTs == null || now - firstTs > IDLE_RESET_TICKS) {
			firstHit.put(effect, now);
		} else if (now - firstTs >= REALTIME_THRESHOLD_TICKS) {
			adapted.add(effect);
			firstHit.remove(effect);
			notifyAdapted(player, effect);
			return false;
		}
		return true;
	}

	private static void notifyAdapted(ServerPlayer player, Holder<MobEffect> effect) {
		String path = effect.unwrapKey().map(k -> k.location().getPath()).orElse("effect");
		player.displayClientMessage(
				Component.translatable("hero.superheroes.doomsday.effect_adapted",
						Component.translatable("effect.minecraft." + path)),
				true);
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
				io.github.grebeshok105.codex.hero.doomsday.sound.DoomsdaySounds.DOOMSDAY_ROAR, SoundSource.PLAYERS, 0.5f, 1.0f);
	}

	public static void clear(ServerPlayer player) {
		UUID id = player.getUUID();
		ADAPTED.remove(id);
		FIRST_HIT_TICK.remove(id);
	}

	private static Set<Holder<MobEffect>> adapted(UUID id) {
		Set<Holder<MobEffect>> s = ADAPTED.get(id);
		if (s == null) {
			s = new HashSet<>();
			ADAPTED.put(id, id, s);
		}
		return s;
	}

	private static Map<Holder<MobEffect>, Long> firstHit(UUID id) {
		Map<Holder<MobEffect>, Long> m = FIRST_HIT_TICK.get(id);
		if (m == null) {
			m = new HashMap<>();
			FIRST_HIT_TICK.put(id, id, m);
		}
		return m;
	}

	private static boolean isDoomsday(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		return data.hasHero() && DOOMSDAY_ID.equals(data.heroId());
	}
}
