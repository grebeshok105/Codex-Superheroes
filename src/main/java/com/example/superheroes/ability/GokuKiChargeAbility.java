package com.example.superheroes.ability;

import com.example.superheroes.effect.GokuKiStackController;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public final class GokuKiChargeAbility implements Ability {
	private static final int TICKS_PER_STACK = 20;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.GOKU_KI_CHARGE;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.9f, 0.7f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(0, Math.max(motion.y, -0.05), 0);
		player.hurtMarked = true;
		player.connection.send(new ClientboundSetEntityMotionPacket(player));

		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 4, true, false, false));

		int stacks = GokuKiStackController.getStacks(player);
		double aRadius = 1.0 + stacks * 0.4;
		for (int i = 0; i < 3 + stacks; i++) {
			double angle = Math.random() * Math.PI * 2;
			double r = aRadius * (0.6 + Math.random() * 0.4);
			level.sendParticles(ModParticles.GOKU_KI_AURA,
					player.getX() + Math.cos(angle) * r,
					player.getY() + Math.random() * 1.8,
					player.getZ() + Math.sin(angle) * r,
					1, 0, 0.05, 0, 0.0);
		}
		if (player.tickCount % 4 == 0) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					player.getX(), player.getY() + 0.5, player.getZ(),
					4, 0.4, 0.7, 0.4, 0.04);
		}

		if (player.tickCount % TICKS_PER_STACK == 0) {
			int oldStacks = GokuKiStackController.getStacks(player);
			int newStacks = GokuKiStackController.addStack(player);
			if (newStacks != oldStacks) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.7f + newStacks * 0.2f);
				level.sendParticles(ParticleTypes.END_ROD,
						player.getX(), player.getY() + 1.0, player.getZ(),
						12, 0.4, 0.6, 0.4, 0.1);
			}
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 0.9f);
	}
}
