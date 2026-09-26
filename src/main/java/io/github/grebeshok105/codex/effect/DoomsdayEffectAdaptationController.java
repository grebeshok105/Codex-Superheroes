package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.hero.DoomsdayHero;
import io.github.grebeshok105.codex.transform.HeroData;
import net.minecraft.core.Holder;
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
import java.util.concurrent.ConcurrentHashMap;

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

	private static final Map<UUID, Set<Holder<MobEffect>>> ADAPTED = new ConcurrentHashMap<>();
	private static final Map<UUID, Map<Holder<MobEffect>, Long>> FIRST_HIT_TICK = new ConcurrentHashMap<>();

	private DoomsdayEffectAdaptationController() {
	}

	public static boolean isTracked(Holder<MobEffect> effect) {
		return TRACKED.contains(effect);
	}

	public static boolean hasAdapted(ServerPlayer player, Holder<MobEffect> effect) {
		Set<Holder<MobEffect>> adapted = ADAPTED.get(player.getUUID());
		return adapted != null && adapted.contains(effect);
	}

	/**
	 * Вызывается при applyEffect mixin-ом.
	 * @return true — эффект разрешить (накатить); false — отменить (адаптация).
	 */
	public static boolean onApply(ServerPlayer player, MobEffectInstance instance) {
		if (!isDoomsday(player)) return true;
		Holder<MobEffect> effect = instance.getEffect();
		if (!TRACKED.contains(effect)) return true;
		Set<Holder<MobEffect>> adapted = ADAPTED.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
		if (adapted.contains(effect)) {
			return false;
		}
		long now = player.serverLevel().getGameTime();
		Map<Holder<MobEffect>, Long> firstHit = FIRST_HIT_TICK.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
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
				io.github.grebeshok105.codex.sound.ModSounds.DOOMSDAY_ROAR, SoundSource.PLAYERS, 0.5f, 1.0f);
	}

	public static void clear(ServerPlayer player) {
		UUID id = player.getUUID();
		ADAPTED.remove(id);
		FIRST_HIT_TICK.remove(id);
	}

	private static boolean isDoomsday(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && DoomsdayHero.ID.equals(data.heroId());
	}
}
