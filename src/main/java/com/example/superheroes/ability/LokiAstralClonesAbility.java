package com.example.superheroes.ability;

import com.example.superheroes.damage.ModDamageTypes;
import com.example.superheroes.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class LokiAstralClonesAbility implements Ability {
	private static final int COOLDOWN_TICKS = 200;
	private static final int CONFUSE_DURATION = 240;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.LOKI_ASTRAL_CLONES;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 50f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();

		AABB scan = player.getBoundingBox().inflate(28.0);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, scan,
				e -> e.isAlive())) {
			mob.setTarget(null);
			mob.addEffect(new MobEffectInstance(MobEffects.CONFUSION, CONFUSE_DURATION, 0, true, true, true));
			mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, CONFUSE_DURATION, 1, true, true, true));
			mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CONFUSE_DURATION, 1, true, true, true));
		}

		AABB pscan = player.getBoundingBox().inflate(20.0);
		for (Player p : level.getEntitiesOfClass(Player.class, pscan,
				e -> e.isAlive() && !e.getUUID().equals(player.getUUID()))) {
			p.hurt(ModDamageTypes.lokiChaos(level, player), 12.0f);
			p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true, true));
			p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true, true));
			p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true, true));
			p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true, true));
			level.sendParticles(ModParticles.PURPLE_FLAME,
					p.getX(), p.getY() + p.getBbHeight() * 0.5, p.getZ(),
					25, 0.4, 0.6, 0.4, 0.04);
		}

		for (int i = 0; i < 3; i++) {
			double angle = i * (Math.PI * 2 / 3);
			double rx = Math.cos(angle) * 1.5;
			double rz = Math.sin(angle) * 1.5;
			level.sendParticles(ParticleTypes.SOUL,
					player.getX() + rx, player.getY() + 1, player.getZ() + rz,
					30, 0.4, 0.8, 0.4, 0.05);
			level.sendParticles(ModParticles.DARK_STAR,
					player.getX() + rx, player.getY() + 1, player.getZ() + rz,
					15, 0.3, 0.6, 0.3, 0.04);
		}

		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 500, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 500, 2, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 500, 1, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 500, 1, true, false, true));

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.4f, 1.0f);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}
}
