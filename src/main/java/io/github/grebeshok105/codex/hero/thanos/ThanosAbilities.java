package io.github.grebeshok105.codex.hero.thanos;

import io.github.grebeshok105.codex.hero.thanos.ability.ThanosCosmicSlamAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosMindPulseAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosRealityTearAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosSnapAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosSoulPulseAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosSpacePortalAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosTimeRewindAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Thanos's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class ThanosAbilities {
	public static final ResourceLocation THANOS_COSMIC_SLAM = ThanosCosmicSlamAbility.ID;
	public static final ResourceLocation THANOS_REALITY_TEAR = ThanosRealityTearAbility.ID;
	public static final ResourceLocation THANOS_MIND_PULSE = ThanosMindPulseAbility.ID;
	public static final ResourceLocation THANOS_TIME_REWIND = ThanosTimeRewindAbility.ID;
	public static final ResourceLocation THANOS_SPACE_PORTAL = ThanosSpacePortalAbility.ID;
	public static final ResourceLocation THANOS_SOUL_PULSE = ThanosSoulPulseAbility.ID;
	public static final ResourceLocation THANOS_SNAP = ThanosSnapAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			THANOS_COSMIC_SLAM,
			THANOS_REALITY_TEAR,
			THANOS_MIND_PULSE,
			THANOS_TIME_REWIND,
			THANOS_SPACE_PORTAL,
			THANOS_SOUL_PULSE,
			THANOS_SNAP
	);

	private ThanosAbilities() {
	}
}
