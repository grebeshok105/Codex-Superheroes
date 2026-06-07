package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.transform.HeroData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;

public final class ReinhardWorthyOpponent {
	private ReinhardWorthyOpponent() {
	}

	/**
	 * Достойный соперник — другой игрок (любой), либо боссы.
	 * Обычные мобы не считаются достойными — против них меч работает как обычный.
	 */
	public static boolean isWorthy(LivingEntity target) {
		if (target == null || !target.isAlive()) return false;
		if (target instanceof Player p) {
			HeroData data = p.getAttachedOrCreate(ModAttachments.HERO_DATA);
			return data.hasHero() || true;
		}
		if (target instanceof WitherBoss) return true;
		if (target instanceof EnderDragon) return true;
		return false;
	}
}
