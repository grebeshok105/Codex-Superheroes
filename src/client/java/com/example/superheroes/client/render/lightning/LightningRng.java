package com.example.superheroes.client.render.lightning;

/**
 * Stafford-variant64 hash-based RNG.
 *
 * Adapted from Fractal Lightning by Builderb0y (MIT).
 * https://github.com/Builderb0y/FractalLightning
 */
public final class LightningRng {
	public static final long PHI64 = -7046029254386353131L;

	private LightningRng() {
	}

	public static long stafford(long z) {
		z = (z ^ z >>> 30) * -4658895280553007687L;
		z = (z ^ z >>> 27) * -7723592293110705685L;
		return z ^ z >>> 31;
	}

	public static long permute(long seed, int salt) {
		return stafford(seed + (long) salt * PHI64);
	}

	public static float toPositiveFloat(long seed) {
		return (float) (seed >>> 40) * 5.9604645E-8F;
	}

	public static float toUniformFloat(long seed) {
		return (float) (seed >> 39) * 5.9604645E-8F;
	}

	public static float nextPositiveFloat(long seed) {
		return toPositiveFloat(stafford(seed));
	}

	public static float nextUniformFloat(long seed) {
		return toUniformFloat(stafford(seed));
	}
}
