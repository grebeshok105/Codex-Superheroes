package io.github.grebeshok105.codex.hero.regulus.runtime;

import com.mojang.serialization.Codec;
import io.github.grebeshok105.codex.core.attachment.AttachmentRegistrar;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** The "second totem chance" flag granted to Regulus when madness starts. */
public final class RegulusBonusLife {
	/**
	 * The attachment type itself lives on this leaf so leaf packages can reach it without
	 * importing the module root; {@code RegulusAttachments.REGULUS_BONUS_LIFE} re-exports it
	 * for module-external readers. Byte-identical to the former
	 * {@code ModAttachments.REGULUS_BONUS_LIFE} registration (id
	 * {@code superheroes:regulus_bonus_life}, FALSE initializer + persistent + copyOnDeath).
	 * Created eagerly at class-init — see {@code RegulusAttachments.init()}.
	 */
	public static final AttachmentType<Boolean> ATTACHMENT =
			AttachmentRegistrar.FABRIC.persistent("regulus_bonus_life", Codec.BOOL, true, () -> Boolean.FALSE);

	private RegulusBonusLife() {
	}
}
