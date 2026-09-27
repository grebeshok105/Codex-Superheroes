package io.github.grebeshok105.codex.hero.sungjinwoo;

import io.github.grebeshok105.codex.hero.sungjinwoo.ability.AriseAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.MonarchsDomainAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.RulersAuthorityAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.SacrificeAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.ShadowExchangeAbility;
import io.github.grebeshok105.codex.hero.sungjinwoo.ability.ShadowExtractionAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Sung Jin-Woo's ability ids — the module's public surface. Leaf ability classes own the
 * canonical {@code ID} constants (leaves must never import the module root), so these are
 * aliases kept byte-identical to the ids that used to live in {@code AbilityIds}.
 */
public final class SungJinwooAbilities {
	public static final ResourceLocation ARISE = AriseAbility.ID;
	public static final ResourceLocation SHADOW_EXCHANGE = ShadowExchangeAbility.ID;
	public static final ResourceLocation SACRIFICE = SacrificeAbility.ID;
	public static final ResourceLocation RULERS_AUTHORITY = RulersAuthorityAbility.ID;
	public static final ResourceLocation SHADOW_EXTRACTION = ShadowExtractionAbility.ID;
	public static final ResourceLocation MONARCHS_DOMAIN = MonarchsDomainAbility.ID;

	public static final List<ResourceLocation> ALL = List.of(
			ARISE,
			SHADOW_EXCHANGE,
			SACRIFICE,
			RULERS_AUTHORITY,
			SHADOW_EXTRACTION,
			MONARCHS_DOMAIN
	);

	private SungJinwooAbilities() {
	}
}
