package io.github.grebeshok105.codex.hero.reinhard;

import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Reinhard's persistent player state — the module's public surface for the attachment.
 * The type itself is created on {@link ReinhardState#ATTACHMENT} (a leaf class, so leaf code
 * never imports the module root); this alias is what gametests and out-of-module code use.
 */
public final class ReinhardAttachments {
	public static final AttachmentType<ReinhardState> STATE = ReinhardState.ATTACHMENT;

	private ReinhardAttachments() {
	}

	/** Forces class initialization during module register so the attachment is created eagerly,
	 *  at the same point in bootstrap where {@code ModAttachments.init()} used to create it. */
	public static void init() {
	}
}
