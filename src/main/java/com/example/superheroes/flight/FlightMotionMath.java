package com.example.superheroes.flight;

public final class FlightMotionMath {
	private static final double EPSILON = 1.0e-4;
	private static final double HOVER_HORIZONTAL_DAMPING = 0.70;
	private static final double HOVER_VERTICAL_DAMPING = 0.35;
	private static final double TAKEOFF_ACCELERATION_MULTIPLIER = 1.35;
	private static final double TAKEOFF_MIN_LIFT = 0.28;
	private static final double BOOST_ACCELERATION_MULTIPLIER = 1.25;
	private static final double LANDING_VERTICAL_DAMPING = 0.55;

	private FlightMotionMath() {
	}

	public static FlightVector next(FlightVector current, FlightControls controls,
			FlightTuning tuning, FlightPhase phase) {
		FlightPhase resolvedPhase = phase == null ? FlightPhase.CRUISE : phase;
		float forward = controls.forward();
		float strafe = controls.strafe();
		if (tuning.forcesForward() && forward < 1.0f) {
			forward = 1.0f;
		}

		double yawRad = Math.toRadians(controls.yawDegrees());
		double pitchRad = Math.toRadians(controls.pitchDegrees());
		double sinYaw = Math.sin(yawRad);
		double cosYaw = Math.cos(yawRad);
		double sinPitch = Math.sin(pitchRad);
		double cosPitch = Math.cos(pitchRad);

		double inputForwardX = -sinYaw * cosPitch;
		double inputForwardY = -sinPitch;
		double inputForwardZ = cosYaw * cosPitch;
		double inputStrafeX = cosYaw;
		double inputStrafeZ = sinYaw;

		double accelX = inputForwardX * forward + inputStrafeX * strafe;
		double accelY = inputForwardY * forward;
		double accelZ = inputForwardZ * forward + inputStrafeZ * strafe;

		if (controls.jumping()) {
			accelY += 1.0;
		}
		if (controls.sneaking()) {
			accelY -= 1.0;
		}
		if (resolvedPhase == FlightPhase.TAKEOFF) {
			accelY += 0.85;
		}

		double phaseAcceleration = tuning.acceleration() * accelerationMultiplier(resolvedPhase);
		double inputMag = Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
		if (inputMag > EPSILON) {
			double scale = phaseAcceleration / Math.max(inputMag, 1.0);
			accelX *= scale;
			accelY *= scale;
			accelZ *= scale;
		} else {
			accelX = 0;
			accelY = 0;
			accelZ = 0;
		}

		double mx = current.x() + accelX;
		double my = current.y() + accelY;
		double mz = current.z() + accelZ;

		if (resolvedPhase == FlightPhase.TAKEOFF && my < TAKEOFF_MIN_LIFT) {
			my = TAKEOFF_MIN_LIFT;
		}
		if (resolvedPhase == FlightPhase.HOVER && !controls.hasHorizontalInput() && !controls.hasVerticalInput()) {
			mx *= HOVER_HORIZONTAL_DAMPING;
			mz *= HOVER_HORIZONTAL_DAMPING;
			my *= HOVER_VERTICAL_DAMPING;
		} else {
			if (!controls.hasHorizontalInput() && !tuning.forcesForward()) {
				mx *= tuning.horizontalFriction();
				mz *= tuning.horizontalFriction();
			}
			if (!controls.hasVerticalInput()) {
				my *= tuning.verticalFriction();
			}
		}
		if (resolvedPhase == FlightPhase.LANDING && my < 0) {
			my *= LANDING_VERTICAL_DAMPING;
		}

		double horizSq = mx * mx + mz * mz;
		double maxHorizontal = tuning.maxHorizontalSpeed();
		if (horizSq > maxHorizontal * maxHorizontal) {
			double scale = maxHorizontal / Math.sqrt(horizSq);
			mx *= scale;
			mz *= scale;
		}
		double maxVertical = tuning.maxVerticalSpeed();
		if (Math.abs(my) > maxVertical) {
			my = Math.signum(my) * maxVertical;
		}
		return new FlightVector(mx, my, mz);
	}

	private static double accelerationMultiplier(FlightPhase phase) {
		return switch (phase) {
			case TAKEOFF -> TAKEOFF_ACCELERATION_MULTIPLIER;
			case BOOST -> BOOST_ACCELERATION_MULTIPLIER;
			case HOVER -> 0.65;
			case LANDING -> 0.80;
			default -> 1.0;
		};
	}
}
