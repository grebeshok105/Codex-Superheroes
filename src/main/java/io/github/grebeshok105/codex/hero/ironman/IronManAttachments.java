package io.github.grebeshok105.codex.hero.ironman;

import io.github.grebeshok105.codex.hero.ironman.runtime.IronManNanoForm;
import io.github.grebeshok105.codex.hero.ironman.runtime.IronManSuitVariant;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Iron Man's player state — the module's public surface for the attachments.
 * The types themselves are created on the runtime leaf classes ({@link IronManNanoForm#ATTACHMENT},
 * {@link IronManSuitVariant#ATTACHMENT}) so leaf code never imports the module root; these
 * aliases are what gametests and out-of-module readers use.
 */
public final class IronManAttachments {
	/** Текущий вариант костюма Железного Человека (индекс в {@link IronManSuitVariant#ALL}). */
	public static final AttachmentType<Integer> SUIT_VARIANT = IronManSuitVariant.ATTACHMENT;
	/** Текущая нано-форма Железного Человека: 0 — нет, 1 — клинок, 2 — супермолот, 3 — щит. */
	public static final AttachmentType<Integer> NANO_FORM = IronManNanoForm.ATTACHMENT;

	private IronManAttachments() {
	}

	/** Forces class initialization during module register so the attachments are created eagerly,
	 *  at the same point in bootstrap where {@code ModAttachments.init()} used to create them. */
	public static void init() {
		IronManSuitVariant.ATTACHMENT.hashCode();
		IronManNanoForm.ATTACHMENT.hashCode();
	}
}
