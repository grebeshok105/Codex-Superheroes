package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.KratosHero;
import io.github.grebeshok105.codex.particle.ModParticles;
import io.github.grebeshok105.codex.transform.HeroData;
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

	public static void register(HeroModuleContext ctx) {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			Entity src = source.getEntity();
			if (!(src instanceof ServerPlayer attacker)) return;
			if (entity == attacker) return;
			if (!isKratos(attacker)) return;
			if (!isBareHand(attacker)) return;
			if (!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) return;
			spawnFx(entity, attacker.serverLevel());
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
