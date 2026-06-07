package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.KratosHero;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.concurrent.ThreadLocalRandom;

public final class KratosHandStrikeFxController {
	private KratosHandStrikeFxController() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			Entity src = source.getEntity();
			if (!(src instanceof ServerPlayer attacker)) return true;
			if (entity == attacker) return true;
			if (!isKratos(attacker)) return true;
			if (!isBareHand(attacker)) return true;
			if (!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) return true;
			spawnFx(entity, attacker.serverLevel());
			return true;
		});
	}

	private static boolean isKratos(ServerPlayer p) {
		HeroData data = p.getAttachedOrCreate(ModAttachments.HERO_DATA);
		return KratosHero.ID.equals(data.heroId());
	}

	private static boolean isBareHand(ServerPlayer p) {
		ItemStack main = p.getMainHandItem();
		return main.isEmpty() || main.is(Items.AIR);
	}

	private static void spawnFx(LivingEntity target, ServerLevel level) {
		int pick = ThreadLocalRandom.current().nextInt(3);
		SimpleParticleType type = switch (pick) {
			case 0 -> ModParticles.KRATOS_HAND_BURST_1;
			case 1 -> ModParticles.KRATOS_HAND_BURST_2;
			default -> ModParticles.KRATOS_HAND_BURST_3;
		};
		double x = target.getX();
		double y = target.getY() + target.getBbHeight() * 0.55;
		double z = target.getZ();
		level.sendParticles(type, x, y, z, 24, 0.45, 0.45, 0.45, 0.08);
		level.sendParticles(ModParticles.PURPLE_FLAME, x, y, z, 10, 0.35, 0.35, 0.35, 0.04);
		level.playSound(null, x, y, z, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 0.9f, 1.4f);
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4f, 1.7f);
	}
}
