package io.github.grebeshok105.codex.hero.naruto.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.hero.naruto.registry.NarutoDamageTypes;
import io.github.grebeshok105.codex.hero.naruto.registry.NarutoParticles;
import io.github.grebeshok105.codex.mechanic.charge.ChargeSession;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NarutoRasenshurikenAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("naruto_rasenshuriken");

	private static final int CHARGE_TICKS = 24;
	private static final int COOLDOWN_TICKS = 600;
	private static final int MAX_LIFE_TICKS = 24;
	private static final double SPEED = 1.25;
	private static final double RADIUS = 3.0;
	private static final float DAMAGE = 18f;

	private static final ChargeSession.Track<ActiveRasenshuriken> SESSION =
			ChargeSession.track(CHARGE_TICKS, MAX_LIFE_TICKS);

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 120f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !SESSION.active(player);
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		SESSION.begin(player, new ActiveRasenshuriken(player));
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BREEZE_CHARGE, SoundSource.PLAYERS, 1.2f, 1.5f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		SESSION.tick(player, NarutoRasenshurikenAbility::tickShuriken);
	}

	private static boolean tickShuriken(ServerPlayer player, ChargeSession.Progress<ActiveRasenshuriken> progress) {
		ActiveRasenshuriken s = progress.payload();
		if (!player.isAlive() || !player.getUUID().equals(s.ownerId)) {
			return false;
		}
		ServerLevel level = player.serverLevel();
		if (!s.launched) {
			s.pos = player.position().add(player.getLookAngle().scale(1.0)).add(0, 1.4, 0);
			s.dir = player.getLookAngle().normalize();
			level.sendParticles(NarutoParticles.NARUTO_RASENGAN_SWIRL,
					s.pos.x, s.pos.y, s.pos.z, 22, 0.45, 0.45, 0.45, 0.08);
			level.sendParticles(ParticleTypes.GUST,
					s.pos.x, s.pos.y, s.pos.z, 8, 0.35, 0.35, 0.35, 0.05);
			if (progress.phase() == CHARGE_TICKS - 1) {
				s.launched = true;
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.4f, 1.2f);
			}
			return true;
		}
		s.pos = s.pos.add(s.dir.scale(SPEED));
		level.sendParticles(NarutoParticles.NARUTO_RASENGAN_SWIRL,
				s.pos.x, s.pos.y, s.pos.z, 28, 0.7, 0.7, 0.7, 0.1);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				s.pos.x, s.pos.y, s.pos.z, 4, 0.8, 0.2, 0.8, 0.0);
		List<LivingEntity> victims = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
				new AABB(s.pos, s.pos).inflate(RADIUS),
				TargetFilters.hostileTo(player)));
		if (!victims.isEmpty()) {
			for (LivingEntity victim : victims) {
				victim.invulnerableTime = 0;
				victim.hurt(NarutoDamageTypes.rasenshuriken(level, player), DAMAGE);
				victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, true, true, true));
				Vec3 push = victim.position().subtract(s.pos).normalize().scale(0.6);
				victim.push(push.x, 0.25, push.z);
			}
			level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
					s.pos.x, s.pos.y, s.pos.z, 1, 0, 0, 0, 0);
			level.playSound(null, s.pos.x, s.pos.y, s.pos.z,
					SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.6f, 0.8f);
			return false;
		}
		return !progress.lastTick();
	}

	private static final class ActiveRasenshuriken {
		private final UUID ownerId;
		private Vec3 pos;
		private Vec3 dir;
		private boolean launched;

		private ActiveRasenshuriken(ServerPlayer player) {
			this.ownerId = player.getUUID();
			this.pos = player.position().add(0, 1.4, 0);
			this.dir = player.getLookAngle();
		}
	}
}
