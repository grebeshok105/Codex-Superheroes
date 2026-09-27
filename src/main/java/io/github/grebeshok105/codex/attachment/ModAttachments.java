package io.github.grebeshok105.codex.attachment;

import io.github.grebeshok105.codex.ModId;

import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeaponIssues;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
	/** Current issue of each bound weapon; not persistent, so a relog or restart invalidates every old copy. */
	public static final AttachmentType<BoundWeaponIssues> BOUND_WEAPON_ISSUES =
			AttachmentRegistry.create(ModId.of("bound_weapon_issues"));



	private ModAttachments() {
	}

	public static void init() {
	}
}
