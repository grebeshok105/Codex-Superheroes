package io.github.grebeshok105.codex.core.attachment;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.AbilityAvailability;
import io.github.grebeshok105.codex.core.lifecycle.ControlLockShadow;
import io.github.grebeshok105.codex.core.lifecycle.ControlLockState;
import io.github.grebeshok105.codex.core.lifecycle.HeldLocks;
import io.github.grebeshok105.codex.core.transform.HeroData;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Attachments shared by core services and more than one hero module
 * ({@link io.github.grebeshok105.codex.attachment.ModAttachments} keeps the
 * per-hero and bound-weapon attachments).
 */
public final class CoreAttachments {
	public static final AttachmentType<HeroData> HERO_DATA = AttachmentRegistry.create(ModId.of("hero_data"), b -> b
			.initializer(() -> HeroData.EMPTY)
			.persistent(HeroData.CODEC)
			.copyOnDeath());

	/**
	 * The public hero id, synced to the owner and every tracking player (audit B14).
	 * Written only by {@link io.github.grebeshok105.codex.core.transform.HeroDataStore}; {@code null}
	 * means "no hero". Mirrors {@code HERO_DATA.heroId} — energy/mana stay private.
	 */
	public static final AttachmentType<ResourceLocation> PUBLIC_HERO = AttachmentRegistry.create(ModId.of("public_hero"), b -> b
			.persistent(ResourceLocation.CODEC)
			.copyOnDeath()
			.syncWith(ResourceLocation.STREAM_CODEC, net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all()));

	/**
	 * Server-computed ability visibility for the owning player's HUD (stage C4 — replaces
	 * the deleted client-side filter). Synced to the owner only; absent means "all
	 * {@link io.github.grebeshok105.codex.core.ability.AbilityAvailability.Visibility#AVAILABLE}".
	 * Written only by {@link io.github.grebeshok105.codex.ability.AbilityAvailabilitySync}.
	 */
	public static final AttachmentType<AbilityAvailability> ABILITY_AVAILABILITY = AttachmentRegistry.create(ModId.of("ability_availability"), b -> b
			.syncWith(AbilityAvailability.STREAM_CODEC, net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.targetOnly()));

	/** Set when energy/mana changed this tick and the client still needs the update. */
	public static final AttachmentType<Boolean> HERO_DATA_RESOURCES_DIRTY =
			AttachmentRegistry.create(ModId.of("hero_data_resources_dirty"));

	/** Live {@link io.github.grebeshok105.codex.core.lifecycle.EntityControlLock} state on a victim entity; not persistent. */
	public static final AttachmentType<ControlLockState> CONTROL_LOCKS =
			AttachmentRegistry.create(ModId.of("control_locks"));

	/** Reverse index on the owning player of the entity locks they hold; not persistent. */
	public static final AttachmentType<HeldLocks> HELD_LOCKS =
			AttachmentRegistry.create(ModId.of("held_locks"));

	/** Persistent record of pre-lock flag values so a reload can restore them; see {@link io.github.grebeshok105.codex.core.lifecycle.EntityControlLock#reconcile}. */
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

	private CoreAttachments() {
	}

	public static void init() {
	}
}
