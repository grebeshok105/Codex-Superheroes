package com.example.superheroes.attachment;

import com.example.superheroes.ModId;
import com.example.superheroes.effect.DoomsdayProgress;
import com.example.superheroes.effect.RaidenState;
import com.example.superheroes.effect.RegulusMadnessState;
import com.example.superheroes.effect.ReinhardState;
import com.example.superheroes.item.bound.BoundWeaponIssues;
import com.example.superheroes.lifecycle.ControlLockShadow;
import com.example.superheroes.lifecycle.ControlLockState;
import com.example.superheroes.lifecycle.HeldLocks;
import com.example.superheroes.transform.HeroData;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

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

	public static final AttachmentType<Boolean> ADMIN_BUILD = AttachmentRegistry.<Boolean>builder()
			.initializer(() -> Boolean.FALSE)
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.buildAndRegister(ModId.of("admin_build"));

	public static final AttachmentType<Integer> SUIT_VARIANT = AttachmentRegistry.<Integer>builder()
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath()
			.buildAndRegister(ModId.of("suit_variant"));

	/** Текущая нано-форма Железного Человека: 0 — нет, 1 — клинок, 2 — супермолот, 3 — щит. */
	public static final AttachmentType<Integer> NANO_FORM = AttachmentRegistry.<Integer>builder()
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath()
			.buildAndRegister(ModId.of("nano_form"));

	/** Set when energy/mana changed this tick and the client still needs the update. */
	public static final AttachmentType<Boolean> HERO_DATA_RESOURCES_DIRTY =
			AttachmentRegistry.create(ModId.of("hero_data_resources_dirty"));

	/** Current issue of each bound weapon; not persistent, so a relog or restart invalidates every old copy. */
	public static final AttachmentType<BoundWeaponIssues> BOUND_WEAPON_ISSUES =
			AttachmentRegistry.create(ModId.of("bound_weapon_issues"));

	/** Live {@link com.example.superheroes.lifecycle.EntityControlLock} state on a victim entity; not persistent. */
	public static final AttachmentType<ControlLockState> CONTROL_LOCKS =
			AttachmentRegistry.create(ModId.of("control_locks"));

	/** Reverse index on the owning player of the entity locks they hold; not persistent. */
	public static final AttachmentType<HeldLocks> HELD_LOCKS =
			AttachmentRegistry.create(ModId.of("held_locks"));

	/** Persistent record of pre-lock flag values so a reload can restore them; see {@link com.example.superheroes.lifecycle.EntityControlLock#reconcile}. */
	public static final AttachmentType<ControlLockShadow> CONTROL_LOCK_SHADOW = AttachmentRegistry.<ControlLockShadow>builder()
			.persistent(ControlLockShadow.CODEC)
			.buildAndRegister(ModId.of("control_lock_shadow"));

	/** Tick of the last hero transform/untransform; not persistent so a new world starts with no cooldown. */
	public static final AttachmentType<Long> TRANSFORM_TICK =
			AttachmentRegistry.create(ModId.of("transform_tick"));

	/**
	 * Ability cooldown deadlines by level game time. Persistent so a hero swap or relog cannot
	 * reset cooldowns (audit B5); intentionally NOT copyOnDeath — death resets cooldowns.
	 */
	public static final AttachmentType<Map<ResourceLocation, Long>> ABILITY_COOLDOWNS = AttachmentRegistry.<Map<ResourceLocation, Long>>builder()
			.persistent(Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG))
			.buildAndRegister(ModId.of("ability_cooldowns"));

	/** Pandora has played her revival cinematic and is permanently un-hittable until she drops the hero. */
	public static final AttachmentType<Boolean> PANDORA_REVIVED = AttachmentRegistry.<Boolean>builder()
			.initializer(() -> Boolean.FALSE)
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.buildAndRegister(ModId.of("pandora_revived"));

	private ModAttachments() {
	}

	public static void init() {
	}
}
