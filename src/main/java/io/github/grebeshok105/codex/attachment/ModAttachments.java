package io.github.grebeshok105.codex.attachment;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.effect.DoomsdayProgress;
import io.github.grebeshok105.codex.effect.RaidenState;
import io.github.grebeshok105.codex.effect.RegulusMadnessState;
import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeaponIssues;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
	public static final AttachmentType<RegulusMadnessState> REGULUS_MADNESS = AttachmentRegistry.create(ModId.of("regulus_madness"), b -> b
			.initializer(() -> RegulusMadnessState.EMPTY));

	public static final AttachmentType<Boolean> REGULUS_BONUS_LIFE = AttachmentRegistry.create(ModId.of("regulus_bonus_life"), b -> b
			.initializer(() -> Boolean.FALSE)
			.persistent(Codec.BOOL)
			.copyOnDeath());

	public static final AttachmentType<DoomsdayProgress> DOOMSDAY_PROGRESS = AttachmentRegistry.create(ModId.of("doomsday_progress"), b -> b
			.initializer(() -> DoomsdayProgress.EMPTY)
			.persistent(DoomsdayProgress.CODEC)
			.copyOnDeath());

	// ВАЖНО: state Райден умышленно НЕ persistent и БЕЗ copyOnDeath —
	// смерть/выход полностью обнуляет таймеры Глаза/Burst, как и просил пользователь.
	public static final AttachmentType<RaidenState> RAIDEN_STATE = AttachmentRegistry.create(ModId.of("raiden_state"), b -> b
			.initializer(() -> RaidenState.EMPTY));

	public static final AttachmentType<Boolean> ADMIN_BUILD = AttachmentRegistry.create(ModId.of("admin_build"), b -> b
			.initializer(() -> Boolean.FALSE)
			.persistent(Codec.BOOL)
			.copyOnDeath());

	public static final AttachmentType<Integer> SUIT_VARIANT = AttachmentRegistry.create(ModId.of("suit_variant"), b -> b
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath());

	/** Текущая нано-форма Железного Человека: 0 — нет, 1 — клинок, 2 — супермолот, 3 — щит. */
	public static final AttachmentType<Integer> NANO_FORM = AttachmentRegistry.create(ModId.of("nano_form"), b -> b
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath());

	/** Current issue of each bound weapon; not persistent, so a relog or restart invalidates every old copy. */
	public static final AttachmentType<BoundWeaponIssues> BOUND_WEAPON_ISSUES =
			AttachmentRegistry.create(ModId.of("bound_weapon_issues"));

	/** Sung Jin-Woo's shadow army: entity UUIDs + summon/phase flags; persistent so a restart re-links shadows (audit B18). */
	public static final AttachmentType<SungShadowArmy> SUNG_SHADOW_ARMY = AttachmentRegistry.create(ModId.of("sung_shadow_army"), b -> b
			.initializer(() -> SungShadowArmy.EMPTY)
			.persistent(SungShadowArmy.CODEC));

	/** Pandora has played her revival cinematic and is permanently un-hittable until she drops the hero. */
	public static final AttachmentType<Boolean> PANDORA_REVIVED = AttachmentRegistry.create(ModId.of("pandora_revived"), b -> b
			.initializer(() -> Boolean.FALSE)
			.persistent(Codec.BOOL)
			.copyOnDeath());

	private ModAttachments() {
	}

	public static void init() {
	}
}
