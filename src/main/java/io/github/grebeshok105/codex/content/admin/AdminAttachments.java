package io.github.grebeshok105.codex.content.admin;

import com.mojang.serialization.Codec;
import io.github.grebeshok105.codex.core.attachment.AttachmentRegistrar;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * The admin module's player attachments. Byte-identical to the former
 * {@code ModAttachments.ADMIN_BUILD} registration (id {@code superheroes:admin_build},
 * persistent + copyOnDeath + FALSE initializer); created eagerly at class-init —
 * see {@link #init()}.
 */
public final class AdminAttachments {
	public static final AttachmentType<Boolean> ADMIN_BUILD =
			AttachmentRegistrar.FABRIC.persistent("admin_build", Codec.BOOL, true, () -> Boolean.FALSE);

	private AdminAttachments() {
	}

	/** Forces class initialization during module register so the attachment is created eagerly,
	 *  at the same point in bootstrap where {@code ModAttachments.init()} used to create it. */
	public static void init() {
	}
}
