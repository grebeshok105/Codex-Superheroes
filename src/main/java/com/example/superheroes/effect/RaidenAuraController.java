package com.example.superheroes.effect;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Транс-аура Райден: пока активен RAIDEN_TRANSCENDENCE, каждые 30 тиков
 * стреляем по ближайшему врагу в радиусе 6 блоков (4 dmg игрокам, 2 мобам).
 */
public final class RaidenAuraController {
	private static final int ZAP_INTERVAL = 30;
	private static final double ZAP_RADIUS = 6.0;
	private static final float ZAP_DAMAGE_PLAYER = 4f;
	private static final float ZAP_DAMAGE_MOB = 2f;

	private RaidenAuraController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!isRaiden(player)) continue;
				tick(player);
			}
		});
	}

	private static boolean isRaiden(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return data.hasHero() && RaidenHero.ID.equals(data.heroId());
	}

	private static void tick(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.activeAbilities().contains(AbilityIds.RAIDEN_TRANSCENDENCE)) return;
		long now = player.serverLevel().getGameTime();
		if (now % ZAP_INTERVAL != 0) return;

		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position().add(0, player.getBbHeight() * 0.5, 0);
		double r2 = ZAP_RADIUS * ZAP_RADIUS;
		AABB box = new AABB(
				origin.x - ZAP_RADIUS, origin.y - ZAP_RADIUS, origin.z - ZAP_RADIUS,
				origin.x + ZAP_RADIUS, origin.y + ZAP_RADIUS, origin.z + ZAP_RADIUS);
		LivingEntity nearest = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !(e instanceof Player p && p.getUUID().equals(player.getUUID()))
						&& e.position().distanceToSqr(origin) <= r2)) {
			double d = le.position().distanceToSqr(origin);
			if (d < bestDist) {
				bestDist = d;
				nearest = le;
			}
		}
		if (nearest == null) return;

		float dmg = (nearest instanceof Player) ? ZAP_DAMAGE_PLAYER : ZAP_DAMAGE_MOB;
		nearest.invulnerableTime = 0;
		nearest.hurt(level.damageSources().playerAttack(player), dmg);

		Vec3 t = nearest.position().add(0, nearest.getBbHeight() * 0.5, 0);
		level.sendParticles(ModParticles.JIWALD_EFFECT,
				t.x, t.y, t.z, 18, 0.4, 0.5, 0.4, 0.25);
		level.sendParticles(ModParticles.BLUE_FLAME,
				t.x, t.y, t.z, 8, 0.3, 0.4, 0.3, 0.04);
		level.sendParticles(ModParticles.FULA_PARTICLE,
				origin.x, origin.y, origin.z, 4, 0.3, 0.3, 0.3, 0.05);
		level.playSound(null, t.x, t.y, t.z,
				SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.7f);

	}
}
