package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.core.attachment.AttachmentRegistrar;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * The little-king heart mark: a transient {@code AttachmentType<UUID>} stamped on a
 * bearer entity with the owning Regulus player's UUID. Transient on purpose (design §1)
 * — the entity itself is the source of truth, so a chunk unload/relog drops the mark and
 * the heart is quietly lost. Created on this leaf so leaf packages never import the
 * module root; {@code RegulusAttachments.REGULUS_HEART_OWNER} re-exports it.
 */
public final class RegulusHeartMark {
	public static final AttachmentType<UUID> ATTACHMENT =
			AttachmentRegistrar.FABRIC.transientType("regulus_heart_owner");

	private RegulusHeartMark() {
	}
}
