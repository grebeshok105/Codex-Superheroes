package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.HomelanderHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Хоумлендер: усиленная регенерация (II) включается, когда HP < {@link #LOW_HP_THRESHOLD},
 * и держится до полного восстановления. После full HP — снимается до уровня базовой пассивной
 * регенерации (которая поддерживается {@link HeroPassiveRegenController}).
 */
public final class HomelanderRegenController {
	private static final float LOW_HP_THRESHOLD = 20.0f;
	private static final int CHECK_INTERVAL = 20;
	private static final int EFFECT_DURATION = 60;

	private static final Set<UUID> ACTIVE = new HashSet<>();

	private HomelanderRegenController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			boolean checkRegen = server.getTickCount() % CHECK_INTERVAL == 0;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
				boolean isHomelander = data.hasHero() && HomelanderHero.ID.equals(data.heroId());
				if (isHomelander) {
					player.getFoodData().setSaturation(0f);
				}
				if (checkRegen) {
					tickPlayer(player, isHomelander);
				}
			}
		});
	}

	private static void tickPlayer(ServerPlayer player, boolean isHomelander) {
		UUID id = player.getUUID();
		if (!isHomelander) {
			ACTIVE.remove(id);
			return;
		}
		float hp = player.getHealth();
		float maxHp = player.getMaxHealth();
		boolean active = ACTIVE.contains(id);
		if (active) {
			if (hp >= maxHp - 0.001f) {
				ACTIVE.remove(id);
			} else {
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION, 1, true, false, true));
			}
		} else if (hp < LOW_HP_THRESHOLD) {
			ACTIVE.add(id);
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION, 1, true, false, true));
		}
	}
}
