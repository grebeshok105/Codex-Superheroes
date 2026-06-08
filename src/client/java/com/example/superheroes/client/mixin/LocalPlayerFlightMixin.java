package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientFlightState;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.flight.FlightAbilityState;
import com.example.superheroes.flight.FlightControls;
import com.example.superheroes.flight.FlightMode;
import com.example.superheroes.flight.FlightMotionMath;
import com.example.superheroes.flight.FlightPhase;
import com.example.superheroes.flight.FlightProfiles;
import com.example.superheroes.flight.FlightTuning;
import com.example.superheroes.flight.FlightVector;
import com.example.superheroes.transform.HeroData;
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
