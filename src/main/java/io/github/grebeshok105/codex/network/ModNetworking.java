package io.github.grebeshok105.codex.network;

import io.github.grebeshok105.codex.core.net.CoreNetworking;
import io.github.grebeshok105.codex.effect.HeroMeleeImpactController;
import io.github.grebeshok105.codex.effect.SuperJumpController;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;


public final class ModNetworking {
	private ModNetworking() {
	}

	public static void init() {
		CoreNetworking.init();

		PayloadTypeRegistry.playC2S().register(SuperJumpC2SPayload.TYPE, SuperJumpC2SPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ReinhardWishConfirmC2SPayload.TYPE, ReinhardWishConfirmC2SPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(HeroMeleeChargeC2SPayload.TYPE, HeroMeleeChargeC2SPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ThinkMarkDashC2SPayload.TYPE, ThinkMarkDashC2SPayload.STREAM_CODEC);

		PayloadTypeRegistry.playS2C().register(FlightStateS2CPayload.TYPE, FlightStateS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(LaserFiredS2CPayload.TYPE, LaserFiredS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(RepulsorBlastS2CPayload.TYPE, RepulsorBlastS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ThanosCosmicBeamS2CPayload.TYPE, ThanosCosmicBeamS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ReactorStateS2CPayload.TYPE, ReactorStateS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(MadnessSyncS2CPayload.TYPE, MadnessSyncS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(MadnessVisualS2CPayload.TYPE, MadnessVisualS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(UraniumPressureS2CPayload.TYPE, UraniumPressureS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(UraniumThreatS2CPayload.TYPE, UraniumThreatS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SungShadowArmyS2CPayload.TYPE, SungShadowArmyS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(DoomsdayProgressS2CPayload.TYPE, DoomsdayProgressS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ThanosStonesS2CPayload.TYPE, ThanosStonesS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(JarvisDetectionS2CPayload.TYPE, JarvisDetectionS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SuitVariantS2CPayload.TYPE, SuitVariantS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(NanoFormS2CPayload.TYPE, NanoFormS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ThinkMarkS2CPayload.TYPE, ThinkMarkS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(AdminBuildS2CPayload.TYPE, AdminBuildS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(KratosRageS2CPayload.TYPE, KratosRageS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(RemDemonismS2CPayload.TYPE, RemDemonismS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ScorpionFxS2CPayload.TYPE, ScorpionFxS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ReinhardWishOptionsS2CPayload.TYPE, ReinhardWishOptionsS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ReinhardCeremonyS2CPayload.TYPE, ReinhardCeremonyS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ReinhardSwordGateS2CPayload.TYPE, ReinhardSwordGateS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ReinhardSwordKillS2CPayload.TYPE, ReinhardSwordKillS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ReinhardDarknessS2CPayload.TYPE, ReinhardDarknessS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ReinhardTimeSlowS2CPayload.TYPE, ReinhardTimeSlowS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(HordeDebugS2CPayload.TYPE, HordeDebugS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(MirrorDimensionS2CPayload.TYPE, MirrorDimensionS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PandoraCinematicS2CPayload.TYPE, PandoraCinematicS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PandoraHouseStateS2CPayload.TYPE, PandoraHouseStateS2CPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(MirrorDimensionStatusC2SPayload.TYPE, MirrorDimensionStatusC2SPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SuperJumpC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			SuperJumpController.activate(player);
		});
		ServerPlayNetworking.registerGlobalReceiver(ReinhardWishConfirmC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			io.github.grebeshok105.codex.ability.ReinhardWishAbility.confirm(player, payload.damageTypeId());
		});
		ServerPlayNetworking.registerGlobalReceiver(HeroMeleeChargeC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			HeroMeleeImpactController.handleChargeInput(player, payload);
		});
		ServerPlayNetworking.registerGlobalReceiver(ThinkMarkDashC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			io.github.grebeshok105.codex.ability.OmnimanThinkMarkAbility.triggerDash(player);
		});
		ServerPlayNetworking.registerGlobalReceiver(MirrorDimensionStatusC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			io.github.grebeshok105.codex.effect.MirrorDimensionController.handleStatus(player, payload.status());
		});
	}

	public static void syncFlightState(ServerPlayer player, io.github.grebeshok105.codex.flight.FlightMode mode,
			io.github.grebeshok105.codex.flight.FlightPhase phase, float horizontalSpeed, boolean active) {
		FlightStateS2CPayload payload = new FlightStateS2CPayload(
				player.getId(), active, mode.ordinal(), phase.ordinal(), horizontalSpeed);
		ServerPlayNetworking.send(player, payload);
		for (ServerPlayer observer : PlayerLookup.tracking(player)) {
			if (observer != player) {
				ServerPlayNetworking.send(observer, payload);
			}
		}
	}


	public static void broadcastLaser(ServerPlayer shooter, Vec3 start, Vec3 end) {
		LaserFiredS2CPayload payload = new LaserFiredS2CPayload(shooter.getUUID(), start, end);
		for (ServerPlayer observer : PlayerLookup.tracking(shooter)) {
			if (observer != shooter) {
				ServerPlayNetworking.send(observer, payload);
			}
		}
	}

	public static void broadcastLaserFromEntity(net.minecraft.world.entity.Entity shooter, Vec3 start, Vec3 end) {
		LaserFiredS2CPayload payload = new LaserFiredS2CPayload(shooter.getUUID(), start, end);
		for (ServerPlayer observer : PlayerLookup.tracking(shooter)) {
			ServerPlayNetworking.send(observer, payload);
		}
	}

	public static void broadcastRepulsor(ServerPlayer shooter, Vec3 start, Vec3 end) {
		RepulsorBlastS2CPayload payload = new RepulsorBlastS2CPayload(shooter.getUUID(), start, end);
		ServerPlayNetworking.send(shooter, payload);
		for (ServerPlayer observer : PlayerLookup.tracking(shooter)) {
			if (observer != shooter) {
				ServerPlayNetworking.send(observer, payload);
			}
		}
	}

	public static void broadcastThanosCosmicBeam(ServerPlayer shooter, Vec3 start, Vec3 end) {
		ThanosCosmicBeamS2CPayload payload = new ThanosCosmicBeamS2CPayload(shooter.getUUID(), start, end);
		ServerPlayNetworking.send(shooter, payload);
		for (ServerPlayer observer : PlayerLookup.tracking(shooter)) {
			if (observer != shooter) {
				ServerPlayNetworking.send(observer, payload);
			}
		}
	}
}
