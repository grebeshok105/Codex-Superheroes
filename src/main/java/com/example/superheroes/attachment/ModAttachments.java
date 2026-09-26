package com.example.superheroes.attachment;

import com.example.superheroes.ModId;
import com.example.superheroes.core.ability.AbilityAvailability;
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
	public static final AttachmentType<HeroData> HERO_DATA = AttachmentRegistry.create(ModId.of("hero_data"), b -> b
			.initializer(() -> HeroData.EMPTY)
			.persistent(HeroData.CODEC)
			.copyOnDeath());

	/**
	 * The public hero id, synced to the owner and every tracking player (audit B14).
	 * Written only by {@link com.example.superheroes.transform.HeroDataStore}; {@code null}
	 * means "no hero". Mirrors {@code HERO_DATA.heroId} — energy/mana stay private.
	 */
	public static final AttachmentType<ResourceLocation> PUBLIC_HERO = AttachmentRegistry.create(ModId.of("public_hero"), b -> b
			.persistent(ResourceLocation.CODEC)
			.copyOnDeath()
			.syncWith(ResourceLocation.STREAM_CODEC, net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all()));

	/**
	 * Server-computed ability visibility for the owning player's HUD (stage C4 — replaces
	 * the deleted client-side filter). Synced to the owner only; absent means "all
	 * {@link com.example.superheroes.core.ability.AbilityAvailability.Visibility#AVAILABLE}".
	 * Written only by {@link com.example.superheroes.ability.AbilityAvailabilitySync}.
	 */
	public static final AttachmentType<AbilityAvailability> ABILITY_AVAILABILITY = AttachmentRegistry.create(ModId.of("ability_availability"), b -> b
			.syncWith(AbilityAvailability.STREAM_CODEC, net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.targetOnly()));

	public static final AttachmentType<RegulusMadnessState> REGULUS_MADNESS = AttachmentRegistry.create(ModId.of("regulus_madness"), b -> b
			.initializer(() -> RegulusMadnessState.EMPTY));

	public static final AttachmentType<Boolean> REGULUS_BONUS_LIFE = AttachmentRegistry.create(ModId.of("regulus_bonus_life"), b -> b
			.initializer(() -> Boolean.FALSE)
			.persistent(Codec.BOOL)
			.copyOnDeath());

	public static final AttachmentType<DoomsdayProgress> DOOMSDAY_PROGRESS = AttachmentRegistry.create(ModId.of("doomsday_progress"), b -> b
			.initializer(() -> DoomsdayProgress.EMPTY)
			.persistent(DoomsdayProgress.CODEC)
			.copyOnDeath());

	public static final AttachmentType<ReinhardState> REINHARD_STATE = AttachmentRegistry.create(ModId.of("reinhard_state"), b -> b
			.initializer(() -> ReinhardState.EMPTY)
			.persistent(ReinhardState.CODEC)
			.copyOnDeath());

	// ВАЖНО: state Райден умышленно НЕ persistent и БЕЗ copyOnDeath —
	// смерть/выход полностью обнуляет таймеры Глаза/Burst, как и просил пользователь.
	public static final AttachmentType<RaidenState> RAIDEN_STATE = AttachmentRegistry.create(ModId.of("raiden_state"), b -> b
			.initializer(() -> RaidenState.EMPTY));

	public static final AttachmentType<Boolean> ADMIN_BUILD = AttachmentRegistry.create(ModId.of("admin_build"), b -> b
			.initializer(() -> Boolean.FALSE)
			.persistent(Codec.BOOL)
			.copyOnDeath());

	public static final AttachmentType<Integer> SUIT_VARIANT = AttachmentRegistry.create(ModId.of("suit_variant"), b -> b
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath());

	/** Текущая нано-форма Железного Человека: 0 — нет, 1 — клинок, 2 — супермолот, 3 — щит. */
	public static final AttachmentType<Integer> NANO_FORM = AttachmentRegistry.create(ModId.of("nano_form"), b -> b
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath());

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
	public static final AttachmentType<ControlLockShadow> CONTROL_LOCK_SHADOW = AttachmentRegistry.create(ModId.of("control_lock_shadow"), b -> b
			.persistent(ControlLockShadow.CODEC));

	/** Tick of the last hero transform/untransform; not persistent so a new world starts with no cooldown. */
	public static final AttachmentType<Long> TRANSFORM_TICK =
			AttachmentRegistry.create(ModId.of("transform_tick"));

	/**
	 * Ability cooldown deadlines by level game time. Persistent so a hero swap or relog cannot
	 * reset cooldowns (audit B5); intentionally NOT copyOnDeath — death resets cooldowns.
	 */
	public static final AttachmentType<Map<ResourceLocation, Long>> ABILITY_COOLDOWNS = AttachmentRegistry.create(ModId.of("ability_cooldowns"), b -> b
			.persistent(Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)));

	/** Sung Jin-Woo's shadow army: entity UUIDs + summon/phase flags; persistent so a restart re-links shadows (audit B18). */
	public static final AttachmentType<SungShadowArmy> SUNG_SHADOW_ARMY = AttachmentRegistry.create(ModId.of("sung_shadow_army"), b -> b
			.initializer(() -> SungShadowArmy.EMPTY)
			.persistent(SungShadowArmy.CODEC));

	/** Pandora has played her revival cinematic and is permanently un-hittable until she drops the hero. */
	public static final AttachmentType<Boolean> PANDORA_REVIVED = AttachmentRegistry.create(ModId.of("pandora_revived"), b -> b
			.initializer(() -> Boolean.FALSE)
			.persistent(Codec.BOOL)
			.copyOnDeath());

	private ModAttachments() {
	}

	public static void init() {
	}
}
