package io.github.grebeshok105.codex.hero.regulus;

import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusBonusLife;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusHeartMark;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessState;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Regulus's player state — the module's public surface for the attachments.
 * The types themselves are created on the leaf classes ({@link RegulusMadnessState#ATTACHMENT},
 * {@link RegulusBonusLife#ATTACHMENT}) so leaf code never imports the module root; these
 * aliases are what gametests and out-of-module readers use.
 */
public final class RegulusAttachments {
	public static final AttachmentType<RegulusMadnessState> REGULUS_MADNESS = RegulusMadnessState.ATTACHMENT;
	public static final AttachmentType<Boolean> REGULUS_BONUS_LIFE = RegulusBonusLife.ATTACHMENT;
	/** The little-king heart mark on bearer entities — transient, entity-held owner UUID. */
	public static final AttachmentType<UUID> REGULUS_HEART_OWNER = RegulusHeartMark.ATTACHMENT;

	private RegulusAttachments() {
	}

	/** Forces class initialization during module register so the attachments are created eagerly,
	 *  at the same point in bootstrap where {@code ModAttachments.init()} used to create them. */
	public static void init() {
		RegulusMadnessState.ATTACHMENT.hashCode();
		RegulusBonusLife.ATTACHMENT.hashCode();
		RegulusHeartMark.ATTACHMENT.hashCode();
	}
}
