package io.github.grebeshok105.codex.hero.captainamerica.ability;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.combat.TargetFilters;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.hero.captainamerica.registry.CaptainAmericaDamageTypes;
import io.github.grebeshok105.codex.hero.captainamerica.registry.CaptainAmericaParticles;
import io.github.grebeshok105.codex.mechanic.charge.ChargeSession;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CapShieldSlamAbility implements Ability {
	public static final ResourceLocation ID = ModId.of("cap_shield_slam");

	private static final int COOLDOWN_TICKS = 200;
	private static final double RADIUS = 5.0;
	private static final float DAMAGE = 5.0f;
	private static final int MAX_AIR_TICKS = 100;
	private static final int MIN_AIR_TICKS_BEFORE_DETONATE = 4;

	// phase() counts air ticks; tick 100 must still reach the body (a touchdown
	// there detonates), so totalTicks is MAX_AIR_TICKS + 1, not MAX_AIR_TICKS.
	private static final ChargeSession.Track<Void> SESSION =
			ChargeSession.track(MAX_AIR_TICKS + 1, 0);

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
		return 80f;
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
		ServerLevel level = player.serverLevel();
		Vec3 motion = new Vec3(0, 1.4, 0);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
		SESSION.begin(player, null);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.PLAYERS, 1.2f, 1.4f);
		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	public static void serverTick(ServerPlayer player) {
		SESSION.tick(player, CapShieldSlamAbility::tickSlam);
	}

	private static boolean tickSlam(ServerPlayer player, ChargeSession.Progress<Void> progress) {
		int airTicks = progress.phase();
		if (airTicks >= MIN_AIR_TICKS_BEFORE_DETONATE && player.onGround()) {
			detonate(player);
			return false;
		}
		return airTicks < MAX_AIR_TICKS;
	}

	private static void detonate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 pos = player.position();

		AABB box = new AABB(
				pos.x - RADIUS, pos.y - 1, pos.z - RADIUS,
				pos.x + RADIUS, pos.y + 2, pos.z + RADIUS);
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box,
				TargetFilters.hostileTo(player))) {
			le.hurt(CaptainAmericaDamageTypes.capShieldSlam(level, player), DAMAGE);
			Vec3 away = le.position().subtract(pos);
			double horizDist = Math.sqrt(away.x * away.x + away.z * away.z);
			if (horizDist < 0.01) horizDist = 0.01;
			Vec3 push = new Vec3(away.x / horizDist * 1.6, 0.8, away.z / horizDist * 1.6);
			le.setDeltaMovement(push);
			le.hurtMarked = true;
		}

		level.sendParticles(CaptainAmericaParticles.CAP_SHIELD_SLAM_BURST,
				pos.x, pos.y + 0.1, pos.z, 80, RADIUS * 0.6, 0.4, RADIUS * 0.6, 0.3);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				pos.x, pos.y + 0.1, pos.z, 50, RADIUS * 0.5, 0.3, RADIUS * 0.5, 0.1);
		level.sendParticles(ParticleTypes.EXPLOSION,
				pos.x, pos.y + 0.5, pos.z, 4, RADIUS * 0.3, 0.1, RADIUS * 0.3, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				pos.x, pos.y + 0.4, pos.z, 6, RADIUS * 0.4, 0.1, RADIUS * 0.4, 0.0);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.4f, 0.6f);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.7f);
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.4f);
	}
}
