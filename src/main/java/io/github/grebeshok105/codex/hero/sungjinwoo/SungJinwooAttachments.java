package io.github.grebeshok105.codex.hero.sungjinwoo;

import io.github.grebeshok105.codex.hero.sungjinwoo.runtime.SungShadowArmy;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Sung Jin-Woo's persistent player state — the module's public surface for the
 * attachment. The type itself is created on {@link SungShadowArmy#ATTACHMENT} (a leaf
 * class, so leaf code never imports the module root); this alias is what gametests and
 * out-of-module code use.
 */
public final class SungJinwooAttachments {
	public static final AttachmentType<SungShadowArmy> ARMY = SungShadowArmy.ATTACHMENT;

	private SungJinwooAttachments() {
	}

	/** Forces class initialization during module register so the attachment is created eagerly,
	 *  at the same point in bootstrap where {@code ModAttachments.init()} used to create it. */
	public static void init() {
	}
}
