package io.github.grebeshok105.codex.hero.homelander.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.model.HeroData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * Хоумлендер: усиленная регенерация (II) включается, когда HP < {@link #LOW_HP_THRESHOLD},
 * и держится до полного восстановления. После full HP — снимается до уровня базовой пассивной
 * регенерации (которая поддерживается {@link io.github.grebeshok105.codex.mechanic.passive.HeroPassiveRegenController}).
 */
public final class HomelanderRegenController {
	private static final net.minecraft.resources.ResourceLocation HOMELANDER_ID = ModId.of("homelander");

	private static final float LOW_HP_THRESHOLD = 20.0f;
	private static final int CHECK_INTERVAL = 20;
	private static final int EFFECT_DURATION = 60;

	// No lifecycle clearOn: tickPlayer already drops the flag when the player is
	// not Homelander or back at full HP — same sites as the old set.
	private static final OwnedSessionMap<UUID, Boolean> ACTIVE =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));

	private HomelanderRegenController() {
	}


	private static void tickPlayer(ServerPlayer player, boolean isHomelander) {
		UUID id = player.getUUID();
		if (!isHomelander) {
			ACTIVE.remove(id);
			return;
		}
		float hp = player.getHealth();
		float maxHp = player.getMaxHealth();
		boolean active = ACTIVE.containsKey(id);
		if (active) {
			if (hp >= maxHp - 0.001f) {
				ACTIVE.remove(id);
			} else {
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION, 1, true, false, true));
			}
		} else if (hp < LOW_HP_THRESHOLD) {
			ACTIVE.put(id, id, Boolean.TRUE);
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION, 1, true, false, true));
		}
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		boolean checkRegen = server.getTickCount() % CHECK_INTERVAL == 0;
		boolean isHomelander = data.hasHero() && HOMELANDER_ID.equals(data.heroId());
		if (isHomelander) {
			player.getFoodData().setSaturation(0f);
		}
		if (checkRegen) {
			tickPlayer(player, isHomelander);
		}
	}

}
