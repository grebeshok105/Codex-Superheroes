package io.github.grebeshok105.codex.attachment;

import io.github.grebeshok105.codex.ModId;

import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeaponIssues;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
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
