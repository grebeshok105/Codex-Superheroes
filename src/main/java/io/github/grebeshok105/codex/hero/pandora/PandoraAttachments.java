package io.github.grebeshok105.codex.hero.pandora;

import io.github.grebeshok105.codex.hero.pandora.runtime.PandoraDeathController;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Pandora's persistent player state — the module's public surface for the attachment.
 * The type itself is created on {@link PandoraDeathController#PANDORA_REVIVED} (a leaf class,
 * so leaf code never imports the module root); this alias is what gametests and
 * out-of-module code use.
 */
public final class PandoraAttachments {
	public static final AttachmentType<Boolean> PANDORA_REVIVED = PandoraDeathController.PANDORA_REVIVED;

	private PandoraAttachments() {
	}

	/** Forces class initialization during module register so the attachment is created eagerly,
	 *  at the same point in bootstrap where {@code ModAttachments.init()} used to create it. */
	public static void init() {
	}
}
