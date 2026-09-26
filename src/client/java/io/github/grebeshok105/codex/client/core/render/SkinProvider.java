package io.github.grebeshok105.codex.client.core.render;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Hero-owned skin choice, registered per hero via {@code HeroClientContext.skin(...)}.
 * Every method returns {@code null} to fall back to the hero's default skin / vanilla model.
 *
 * <p>A provider owns the whole check — the resolver and the mixins know no heroes. Each method
 * must preserve the exact conditions that lived in its call site before CL4:
 *
 * <ul>
 *   <li>{@link #skin} replaces the branches of {@code AbstractClientPlayerSkinMixin#getSkin}
 *       (body/portrait texture seen by everyone):
 *       <ul>
 *         <li>Homelander: {@code ClientUraniumPressureState.isPressured(uuid)} → wounded texture
 *             ({@code textures/entity/hero/infected_homelander_wounded.png});</li>
 *         <li>Sung: {@code ClientShadowArmyState.hasShadows(uuid)} → {@code SungJinwooHero.SKIN_PHASE_2};</li>
 *         <li>Thanos: {@code ThanosSkinTextures.textureFor(ClientThanosState.maskFor(uuid))};</li>
 *         <li>Iron Man: {@code IronManSuitVariant.get(ClientSuitVariantState.variantFor(uuid)).texture()};</li>
 *         <li>any other state → return {@code null} so the hero's {@code getSkinTexture()} applies.</li>
 *       </ul></li>
 *   <li>{@link #handSkin} replaces the branches of {@code PlayerRendererMixin#renderHand}
 *       (first-person arm): Sung {@code ClientShadowArmyState.isPhase2(uuid)} → {@code SKIN_PHASE_2},
 *       Iron Man suit variant, Thanos mask — the same checks as {@link #skin} for Iron Man/Thanos,
 *       but Sung deliberately uses {@code isPhase2} here versus {@code hasShadows} for the body;
 *       that divergence existed before CL4 and is kept as-is (realigning it is a separate
 *       hero-owner decision). No Homelander branch existed for the hand — do not add one.</li>
 *   <li>{@link #slimModel} picks the {@code PlayerSkin.Model}; {@code null} keeps the historical
 *       default (WIDE) that the old mixin hardcoded for every hero.</li>
 * </ul>
 *
 * <p>The nano suit-up suppression ({@code ClientNanoSuitUpState.suppressHeroSkin}) is a core filter
 * evaluated by the mixins before the resolver runs — providers must not re-check it.
 */
public interface SkinProvider {
	/**
	 * Texture used in place of the player's real skin ({@code AbstractClientPlayer#getSkin}),
	 * or {@code null} to use the hero's {@code getSkinTexture()}.
	 */
	@Nullable
	ResourceLocation skin(AbstractClientPlayer player, ResourceLocation heroId);

	/**
	 * Texture for the first-person arm ({@code PlayerRenderer#renderHand}, local player only),
	 * or {@code null} to use the hero's {@code getSkinTexture()}.
	 */
	@Nullable
	default ResourceLocation handSkin(AbstractClientPlayer player, ResourceLocation heroId) {
		return null;
	}

	/**
	 * {@code TRUE} → slim arm model, {@code FALSE} → wide; {@code null} keeps the existing default (wide).
	 */
	@Nullable
	default Boolean slimModel(AbstractClientPlayer player, ResourceLocation heroId) {
		return null;
	}
}
