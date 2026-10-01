package io.github.grebeshok105.codex.hero.regulus.runtime;

import com.mojang.serialization.Codec;
import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

/** The "second totem chance" flag granted to Regulus when madness starts. */
public final class RegulusBonusLife {
	/**
	 * The attachment type itself lives on this leaf so leaf packages can reach it without
	 * importing the module root; {@code RegulusAttachments.REGULUS_BONUS_LIFE} re-exports it
	 * for module-external readers. Same semantics as the former registration (id
	 * {@code superheroes:regulus_bonus_life}, FALSE initializer + persistent + copyOnDeath),
	 * plus {@code targetOnly} sync — the owning player's HUD reads the flag straight off the
	 * attachment instead of the deleted {@code madness_sync} payload. The AttachmentRegistrar
	 * seam has no syncWith overload, so this is a direct {@link AttachmentRegistry#create}.
	 * Created eagerly at class-init — see {@code RegulusAttachments.init()}.
	 */
	public static final AttachmentType<Boolean> ATTACHMENT =
			AttachmentRegistry.create(ModId.of("regulus_bonus_life"), b -> b
					.initializer(() -> Boolean.FALSE)
					.persistent(Codec.BOOL)
					.copyOnDeath()
					.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly()));

	private RegulusBonusLife() {
	}
}
