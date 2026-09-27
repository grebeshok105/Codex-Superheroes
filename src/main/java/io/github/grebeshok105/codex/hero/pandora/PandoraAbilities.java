package io.github.grebeshok105.codex.hero.pandora;

import io.github.grebeshok105.codex.hero.pandora.ability.MirrorDimensionAbility;
import io.github.grebeshok105.codex.hero.pandora.ability.MirrorModeCycleAbility;
import io.github.grebeshok105.codex.hero.pandora.ability.SpaceCrushAbility;
import io.github.grebeshok105.codex.hero.pandora.ability.SpatialBindAbility;
import io.github.grebeshok105.codex.hero.pandora.ability.VanityStripAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Pandora's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class PandoraAbilities {
	public static final ResourceLocation MIRROR_DIMENSION = MirrorDimensionAbility.ID;
	public static final ResourceLocation MIRROR_MODE_CYCLE = MirrorModeCycleAbility.ID;
	public static final ResourceLocation SPATIAL_BIND = SpatialBindAbility.ID;
	public static final ResourceLocation SPACE_CRUSH = SpaceCrushAbility.ID;
	public static final ResourceLocation VANITY_STRIP = VanityStripAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			MIRROR_DIMENSION,
			MIRROR_MODE_CYCLE,
			SPATIAL_BIND,
			SPACE_CRUSH,
			VANITY_STRIP
	);

	private PandoraAbilities() {
	}
}
