package io.github.grebeshok105.codex.hero.loki;

import io.github.grebeshok105.codex.hero.loki.ability.LokiAstralClonesAbility;
import io.github.grebeshok105.codex.hero.loki.ability.LokiChaosBoltAbility;
import io.github.grebeshok105.codex.hero.loki.ability.LokiGlamourAbility;
import io.github.grebeshok105.codex.hero.loki.ability.LokiMindCharmAbility;
import io.github.grebeshok105.codex.hero.loki.ability.LokiTesseractBlinkAbility;
import net.minecraft.resources.ResourceLocation;

/**
 * Loki's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class LokiAbilities {
	public static final ResourceLocation LOKI_ASTRAL_CLONES = LokiAstralClonesAbility.ID;
	public static final ResourceLocation LOKI_TESSERACT_BLINK = LokiTesseractBlinkAbility.ID;
	public static final ResourceLocation LOKI_MIND_CHARM = LokiMindCharmAbility.ID;
	public static final ResourceLocation LOKI_GLAMOUR = LokiGlamourAbility.ID;
	public static final ResourceLocation LOKI_CHAOS_BOLT = LokiChaosBoltAbility.ID;

	private LokiAbilities() {
	}
}
