package io.github.grebeshok105.codex.hero.doomsday;

import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayProgress;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Doomsday's persistent player state — the module's public surface for the attachment.
 * The type itself is created on {@link DoomsdayProgress#ATTACHMENT} (a leaf class, so leaf code
 * never imports the module root); this alias is what gametests and out-of-module code use.
 */
public final class DoomsdayAttachments {
	public static final AttachmentType<DoomsdayProgress> PROGRESS = DoomsdayProgress.ATTACHMENT;

	private DoomsdayAttachments() {
	}

	/** Forces class initialization during module register so the attachment is created eagerly
	 *  (it used to be created inside {@code ModAttachments.init()} at bootstrap). */
	public static void init() {
	}
}
