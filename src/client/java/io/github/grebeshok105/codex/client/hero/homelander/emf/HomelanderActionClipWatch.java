package io.github.grebeshok105.codex.client.hero.homelander.emf;

import io.github.grebeshok105.codex.hero.homelander.HomelanderItems;
import io.github.grebeshok105.codex.hero.homelander.item.MilkBottleItem;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * End-fade and interrupt handling for one-shot action clips.
 *
 * <p>{@code loop=false} clips never auto-fade in {@link EmfPlaybackState}: the
 * blend weight would sit at 1 on the last frame forever — for
 * {@code milk_drink} that leaves the {@code milk_bottle}/{@code milk_cap}/
 * {@code mouth_open} props visible. VFX factories register a watch here; each
 * client tick the watch fades the clip's target weight to 0 — the same
 * write as {@code HomelanderPoseApi.stopClip}, inlined so this package does
 * not depend back on the flight package — once {@code oneShotFinished}; or,
 * for {@code milk_drink}, the moment the player stops using the milk bottle
 * before the sip frame (a released right click = interrupted drink: props
 * hide, no leftover).
 *
 * <p>The interrupt check arms only after the use-item state has actually been
 * observed ({@code sawUse}): entity use-state sync lags the VFX packet by a
 * few ticks and the clip must not fade during that handshake window. A clip
 * that never sees the use state still end-fades normally.
 */
public final class HomelanderActionClipWatch {
	private static final Map<Integer, Watch> WATCHES = new ConcurrentHashMap<>();
	/** Clip frame at which {@code finishUsingItem} fires — the sip beat. */
	private static final float SIP_FRAME = MilkBottleItem.DRINK_TICKS * EmfPlaybackState.FRAMES_PER_TICK;

	private HomelanderActionClipWatch() {
	}

	/** Fade {@code clip} to 0 once its one-shot playback reaches the last frame. */
	public static void watchEndFade(int entityId, ResourceLocation clip) {
		WATCHES.put(entityId, new Watch(clip, false));
	}

	/** {@link #watchEndFade} plus the pre-sip interrupt fade for the milk drink. */
	public static void watchMilkDrink(int entityId, ResourceLocation clip) {
		WATCHES.put(entityId, new Watch(clip, true));
	}

	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null) {
			WATCHES.clear();
			return;
		}
		Iterator<Map.Entry<Integer, Watch>> it = WATCHES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Watch> entry = it.next();
			Watch watch = entry.getValue();
			HomelanderEmfRuntime runtime = HomelanderEmfRuntime.peek(entry.getKey());
			if (runtime == null) {
				it.remove();
				continue;
			}
			String name = clipName(watch.clip);
			boolean fade = runtime.playback().oneShotFinished(name);
			if (!fade && watch.milk) {
				Entity entity = level.getEntity(entry.getKey());
				boolean drinking = entity instanceof LivingEntity le && le.isUsingItem()
						&& le.getUseItem().is(HomelanderItems.MILK_BOTTLE);
				if (drinking) {
					watch.sawUse = true;
				} else if (watch.sawUse && runtime.playback().time(name) < SIP_FRAME) {
					// Released before the sip — interrupted drink. Past the sip
					// frame the item finished normally and the clip's authored
					// outro plays out until the end-fade.
					fade = true;
				}
			}
			if (fade) {
				runtime.playback().setTargetWeight(name, 0f);
				it.remove();
			} else if (runtime.playback().target(name) <= 0f) {
				// Another action clip took the single action slot and faded
				// this one out — nothing left to watch.
				it.remove();
			}
		}
	}

	private static String clipName(ResourceLocation clip) {
		String path = clip.getPath();
		return path.substring(path.lastIndexOf('/') + 1);
	}

	private static final class Watch {
		final ResourceLocation clip;
		final boolean milk;
		boolean sawUse;

		Watch(ResourceLocation clip, boolean milk) {
			this.clip = clip;
			this.milk = milk;
		}
	}
}
