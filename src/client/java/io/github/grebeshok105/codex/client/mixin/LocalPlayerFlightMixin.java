package io.github.grebeshok105.codex.client.mixin;

import io.github.grebeshok105.codex.client.ClientFlightState;
import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.flight.FlightAbilityState;
import io.github.grebeshok105.codex.flight.FlightControls;
import io.github.grebeshok105.codex.flight.FlightMode;
import io.github.grebeshok105.codex.flight.FlightMotionMath;
import io.github.grebeshok105.codex.flight.FlightPhase;
import io.github.grebeshok105.codex.flight.FlightProfiles;
import io.github.grebeshok105.codex.flight.FlightTuning;
import io.github.grebeshok105.codex.flight.FlightVector;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class LocalPlayerFlightMixin {
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void superheroes$inertialFlight(Vec3 input, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (!self.level().isClientSide || !(self instanceof LocalPlayer player)) {
			return;
		}
		HeroData heroData = ClientHeroState.data();
		FlightMode mode = FlightAbilityState.activeMode(heroData);
		FlightPhase phase = FlightPhase.CRUISE;
		ClientFlightState.State state = ClientFlightState.get(player.getId());
		if (state != null) {
			mode = state.mode();
			phase = state.phase();
		}
		if (mode == null) {
			return;
		}

		FlightTuning tuning = FlightProfiles.tuning(mode, heroData.energy(), ClientHeroState.energyMax(), ModEffects.isMadness(player));
		Vec3 motion = self.getDeltaMovement();
		FlightControls controls = new FlightControls(
				player.getYRot(),
				player.getXRot(),
				player.zza,
				player.xxa,
				player.input != null && player.input.jumping,
				player.input != null && player.input.shiftKeyDown);
		FlightVector next = FlightMotionMath.next(new FlightVector(motion.x, motion.y, motion.z), controls, tuning, phase);

		self.setDeltaMovement(next.x(), next.y(), next.z());
		self.move(MoverType.SELF, self.getDeltaMovement());
		ci.cancel();
	}

}
