package com.example.superheroes.attachment;

import com.example.superheroes.ModId;
import com.example.superheroes.effect.DoomsdayProgress;
import com.example.superheroes.effect.RaidenState;
import com.example.superheroes.effect.RegulusMadnessState;
import com.example.superheroes.effect.ReinhardState;
import com.example.superheroes.transform.HeroData;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
	public static final AttachmentType<HeroData> HERO_DATA = AttachmentRegistry.<HeroData>builder()
			.initializer(() -> HeroData.EMPTY)
			.persistent(HeroData.CODEC)
			.copyOnDeath()
			.buildAndRegister(ModId.of("hero_data"));

	public static final AttachmentType<RegulusMadnessState> REGULUS_MADNESS = AttachmentRegistry.<RegulusMadnessState>builder()
			.initializer(() -> RegulusMadnessState.EMPTY)
			.buildAndRegister(ModId.of("regulus_madness"));

	public static final AttachmentType<Boolean> REGULUS_BONUS_LIFE = AttachmentRegistry.<Boolean>builder()
			.initializer(() -> Boolean.FALSE)
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.buildAndRegister(ModId.of("regulus_bonus_life"));

	public static final AttachmentType<DoomsdayProgress> DOOMSDAY_PROGRESS = AttachmentRegistry.<DoomsdayProgress>builder()
			.initializer(() -> DoomsdayProgress.EMPTY)
			.persistent(DoomsdayProgress.CODEC)
			.copyOnDeath()
			.buildAndRegister(ModId.of("doomsday_progress"));

	public static final AttachmentType<ReinhardState> REINHARD_STATE = AttachmentRegistry.<ReinhardState>builder()
			.initializer(() -> ReinhardState.EMPTY)
			.persistent(ReinhardState.CODEC)
			.copyOnDeath()
			.buildAndRegister(ModId.of("reinhard_state"));

	// ВАЖНО: state Райден умышленно НЕ persistent и БЕЗ copyOnDeath —
	// смерть/выход полностью обнуляет таймеры Глаза/Burst, как и просил пользователь.
	public static final AttachmentType<RaidenState> RAIDEN_STATE = AttachmentRegistry.<RaidenState>builder()
			.initializer(() -> RaidenState.EMPTY)
			.buildAndRegister(ModId.of("raiden_state"));

	private ModAttachments() {
	}

	public static void init() {
	}
}
