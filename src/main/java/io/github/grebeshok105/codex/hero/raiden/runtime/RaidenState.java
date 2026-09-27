package io.github.grebeshok105.codex.hero.raiden.runtime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Состояние Райден Сёгун (Архонт Электро).
 *
 * eyeExpireTick — Глаз Грозного Суда активен до этого тика; 0 = не активен.
 * burstExpireTick — окно Musou Shinsetsu активно до этого тика; 0 = не активно.
 * burstFinalSlashTick — на этом тике сработает финальный AoE-слэш Burst-а; 0 = нет.
 * swordDrawn — true если Ямато сейчас вызвана через способность Manifest Yamato.
 * transcendenceUntilTick — пассивная аура Transcendence активна до этого тика; 0 = не активна.
 * plungingArmedUntilTick — окно «приземление с громом» после Plunging Attack; 0 = не активно.
 */
public record RaidenState(
		long eyeExpireTick,
		long burstExpireTick,
		long burstFinalSlashTick,
		boolean swordDrawn,
		long transcendenceUntilTick,
		long plungingArmedUntilTick
) {
	public static final RaidenState EMPTY = new RaidenState(0L, 0L, 0L, false, 0L, 0L);

	public static final Codec<RaidenState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.optionalFieldOf("eye_expire_tick", 0L).forGetter(RaidenState::eyeExpireTick),
			Codec.LONG.optionalFieldOf("burst_expire_tick", 0L).forGetter(RaidenState::burstExpireTick),
			Codec.LONG.optionalFieldOf("burst_final_slash_tick", 0L).forGetter(RaidenState::burstFinalSlashTick),
			Codec.BOOL.optionalFieldOf("sword_drawn", false).forGetter(RaidenState::swordDrawn),
			Codec.LONG.optionalFieldOf("transcendence_until_tick", 0L).forGetter(RaidenState::transcendenceUntilTick),
			Codec.LONG.optionalFieldOf("plunging_armed_until_tick", 0L).forGetter(RaidenState::plungingArmedUntilTick)
	).apply(instance, RaidenState::new));

	/**
	 * The state attachment itself — created on this leaf so leaf code never imports the module
	 * root; {@code RaidenAttachments.STATE} re-exports it for module-external readers.
	 * Byte-identical to the former {@code ModAttachments.RAIDEN_STATE} registration
	 * (id {@code superheroes:raiden_state}, initializer, deliberately NOT persistent and
	 * without copyOnDeath — death/leave fully resets Eye/Burst timers, as requested).
	 * The AttachmentRegistrar seam has no transient-with-initializer overload, so this stays
	 * a direct {@link AttachmentRegistry#create} call like {@code ModAttachments} had.
	 * Created eagerly at class-init — see {@code RaidenAttachments.init()}.
	 */
	public static final AttachmentType<RaidenState> ATTACHMENT =
			AttachmentRegistry.create(ModId.of("raiden_state"), b -> b
					.initializer(() -> RaidenState.EMPTY));

	public RaidenState withEyeExpireTick(long v) {
		return new RaidenState(v, burstExpireTick, burstFinalSlashTick, swordDrawn, transcendenceUntilTick, plungingArmedUntilTick);
	}

	public RaidenState withBurstExpireTick(long v) {
		return new RaidenState(eyeExpireTick, v, burstFinalSlashTick, swordDrawn, transcendenceUntilTick, plungingArmedUntilTick);
	}

	public RaidenState withBurstFinalSlashTick(long v) {
		return new RaidenState(eyeExpireTick, burstExpireTick, v, swordDrawn, transcendenceUntilTick, plungingArmedUntilTick);
	}

	public RaidenState withSwordDrawn(boolean v) {
		return new RaidenState(eyeExpireTick, burstExpireTick, burstFinalSlashTick, v, transcendenceUntilTick, plungingArmedUntilTick);
	}

	public RaidenState withTranscendenceUntilTick(long v) {
		return new RaidenState(eyeExpireTick, burstExpireTick, burstFinalSlashTick, swordDrawn, v, plungingArmedUntilTick);
	}

	public RaidenState withPlungingArmedUntilTick(long v) {
		return new RaidenState(eyeExpireTick, burstExpireTick, burstFinalSlashTick, swordDrawn, transcendenceUntilTick, v);
	}
}
