package io.github.grebeshok105.codex.hero.regulus.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.regulus.net.HeartsSyncS2CPayload;
import io.github.grebeshok105.codex.hero.regulus.registry.RegulusDamageTypes;
import io.github.grebeshok105.codex.tag.ModEntityTypeTags;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * "Little king" hearts (rework design §1): peaceful/neutral vanilla entities near the
 * Regulus player quietly become heart bearers — the heart simply ends up inside them.
 *
 * <p>Source of truth is the transient {@link RegulusHeartMark#ATTACHMENT} on the bearer;
 * the owner-side {@link OwnedSessionMap} set is a speed cache for count/scales. Rules:
 * {@code minecraft}-namespace entity types only, radius 20 blocks, never a {@link Player},
 * never an {@link Enemy}, never already marked, never in {@code superheroes:heartless};
 * cap {@value #CAP}. Bearer death burns the heart and backlashes the owner (10% max-hp
 * {@code regulus_heart_backlash} true damage + Weakness I {@value #BACKLASH_WEAKNESS_TICKS}t
 * + actionbar); bearer unload/despawn loses the heart quietly. Owner leave/death/
 * hero-clear drops everything via {@link #dropAll(UUID)}.
 */
public final class RegulusHearts {
	public static final int CAP = 12;
	public static final ResourceLocation HEARTS_DAMAGE_MODIFIER_ID =
			ModId.of("modifiers/regulus/hearts_damage");

	private static final ResourceLocation REGULUS_ID = ModId.of("regulus");
	private static final ResourceLocation LION_HEART_ID = ModId.of("lion_heart");
	private static final int SCAN_INTERVAL_TICKS = 20;
	private static final double SCAN_RADIUS = 20.0;
	private static final float BACKLASH_MAX_HEALTH_FRACTION = 0.10f;
	private static final int BACKLASH_WEAKNESS_TICKS = 60;

	/** owner UUID → live bearer UUIDs; manual lifecycle so dropAll strips marks before the set goes. */
	private static final OwnedSessionMap<UUID, Set<UUID>> HEARTS =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));
	/** owner UUID → last sent view + pending overheat counter (Task 4 writes it). */
	private static final OwnedSessionMap<UUID, SyncState> SYNC =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.noneOf(ClearOn.class));
	/** Last known server — captured by the first owner tick; null before any hearts exist. */
	private static MinecraftServer server;

	private RegulusHearts() {
	}

	private record View(List<Integer> ids, boolean lionHeartActive, int overheatTicks) {
	}

	private static final class SyncState {
		private View lastSent;
		private int overheatTicks;
	}

	/** Live hearts owned by this player. */
	public static int count(ServerPlayer owner) {
		return count(owner.getUUID());
	}

	private static int count(UUID ownerId) {
		Set<UUID> set = HEARTS.get(ownerId);
		return set == null ? 0 : set.size();
	}

	/** Total melee/ability damage multiplier: {@code 1.0 + 0.02 * count}. */
	public static float damageScale(ServerPlayer owner) {
		return 1.0f + 0.02f * count(owner);
	}

	/** Extra energy regen per tick from hearts: {@code min(1.8, 0.15 * count)}. */
	public static float energyRegenBonus(ServerPlayer owner) {
		return Math.min(1.8f, 0.15f * count(owner));
	}

	/** Lion-heart free window: {@code 60 + 40 * count} ticks (cap 540 at 12 hearts). */
	public static int heartWindowTicks(ServerPlayer owner) {
		return 60 + 40 * count(owner);
	}

	/**
	 * Task 4 seam — LionHeartController feeds the owner's current overheat through here;
	 * the value goes out in the next {@link HeartsSyncS2CPayload} under the dirty rule.
	 */
	public static void setOverheatTicks(ServerPlayer owner, int ticks) {
		syncState(owner.getUUID()).overheatTicks = Math.max(0, ticks);
	}

	/** The overheat value in the last packet actually sent to this owner (-1 if none yet). */
	public static int syncedOverheatTicks(ServerPlayer owner) {
		SyncState sync = SYNC.get(owner.getUUID());
		return sync == null || sync.lastSent == null ? -1 : sync.lastSent.overheatTicks();
	}

	/**
	 * Drops every heart this owner holds: strips {@code REGULUS_HEART_OWNER} from all
	 * still-loaded bearers BEFORE the owner-set is cleared, refreshes the damage scale,
	 * and pushes a final empty view so the client highlight dies with the session.
	 */
	public static void dropAll(UUID ownerId) {
		dropAll(server, ownerId);
	}

	private static void dropAll(MinecraftServer srv, UUID ownerId) {
		if (srv != null) {
			server = srv;
			Set<UUID> set = HEARTS.get(ownerId);
			if (set != null) {
				for (ServerLevel level : srv.getAllLevels()) {
					for (UUID bearerId : set) {
						Entity bearer = level.getEntity(bearerId);
						if (bearer != null) {
							bearer.removeAttached(RegulusHeartMark.ATTACHMENT);
						}
					}
				}
			}
			ServerPlayer owner = srv.getPlayerList().getPlayer(ownerId);
			if (owner != null && !owner.hasDisconnected()) {
				refreshDamageScale(owner);
				ServerPlayNetworking.send(owner, new HeartsSyncS2CPayload(List.of(), false, 0));
			}
		}
		HEARTS.remove(ownerId);
		SYNC.remove(ownerId);
	}

	public static void register(HeroModuleContext ctx) {
		ctx.ticks().hero(REGULUS_ID, RegulusHearts::tickOwner);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> RegulusHearts.onBearerDeath(entity));
		ServerEntityEvents.ENTITY_UNLOAD.register(RegulusHearts::onBearerUnload);
		ctx.lifecycle().onLeave(player -> RegulusHearts.dropAll(player.getServer(), player.getUUID()));
		ctx.lifecycle().onDeath(player -> RegulusHearts.dropAll(player.getServer(), player.getUUID()));
		ctx.lifecycle().onHeroClear(player -> RegulusHearts.dropAll(player.getServer(), player.getUUID()));
		ctx.lifecycle().onServerStopped(stopped -> server = null);
	}

	private static void tickOwner(MinecraftServer srv, ServerPlayer owner, HeroData data) {
		server = srv;
		if (owner.level().getGameTime() % SCAN_INTERVAL_TICKS != 0L) {
			return;
		}
		collect(owner);
		syncView(owner, data);
	}

	/** The claim scan: any eligible vanilla mob in the aura joins until the cap bites. */
	private static void collect(ServerPlayer owner) {
		ServerLevel level = owner.serverLevel();
		Set<UUID> set = hearts(owner.getUUID());
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
				owner.getBoundingBox().inflate(SCAN_RADIUS), RegulusHearts::eligible)) {
			if (set.size() >= CAP) {
				break;
			}
			e.setAttached(RegulusHeartMark.ATTACHMENT, owner.getUUID());
			set.add(e.getUUID());
		}
		refreshDamageScale(owner);
	}

	private static boolean eligible(LivingEntity e) {
		return e.isAlive()
				&& !(e instanceof Player)
				&& !(e instanceof Enemy)
				&& e.getAttached(RegulusHeartMark.ATTACHMENT) == null
				&& "minecraft".equals(EntityType.getKey(e.getType()).getNamespace())
				&& !e.getType().is(ModEntityTypeTags.HEARTLESS);
	}

	/** Bearer death: heart burns, owner eats the backlash (silent when offline). */
	private static void onBearerDeath(LivingEntity entity) {
		UUID ownerId = entity.getAttached(RegulusHeartMark.ATTACHMENT);
		if (ownerId == null) {
			return;
		}
		Set<UUID> set = HEARTS.get(ownerId);
		if (set != null) {
			set.remove(entity.getUUID());
		}
		MinecraftServer server = entity.getServer();
		ServerPlayer owner = server == null ? null : server.getPlayerList().getPlayer(ownerId);
		if (owner == null || owner.hasDisconnected()) {
			return;
		}
		refreshDamageScale(owner);
		owner.hurt(RegulusDamageTypes.heartBacklash(owner.serverLevel()),
				owner.getMaxHealth() * BACKLASH_MAX_HEALTH_FRACTION);
		owner.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BACKLASH_WEAKNESS_TICKS, 0));
		owner.displayClientMessage(Component.translatable("superheroes.regulus.heart_lost"), true);
	}

	/** Unload/despawn: the heart is gone quietly — the mark itself dies with the entity. */
	private static void onBearerUnload(Entity entity, ServerLevel level) {
		UUID ownerId = entity.getAttached(RegulusHeartMark.ATTACHMENT);
		if (ownerId == null) {
			return;
		}
		Set<UUID> set = HEARTS.get(ownerId);
		if (set != null && set.remove(entity.getUUID())) {
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
			if (owner != null) {
				refreshDamageScale(owner);
			}
		}
	}

	/** Unicast the owner view only when any field changed (ids, lion flag or overheat). */
	private static void syncView(ServerPlayer owner, HeroData data) {
		SyncState sync = syncState(owner.getUUID());
		View view = new View(bearerIds(owner), data.isActive(LION_HEART_ID),
				sync.overheatTicks);
		if (view.equals(sync.lastSent)) {
			return;
		}
		ServerPlayNetworking.send(owner,
				new HeartsSyncS2CPayload(view.ids(), view.lionHeartActive(), view.overheatTicks()));
		sync.lastSent = view;
	}

	/** Bearer network ids for the client lookup — sorted for a stable wire image. */
	private static List<Integer> bearerIds(ServerPlayer owner) {
		Set<UUID> set = HEARTS.get(owner.getUUID());
		if (set == null || set.isEmpty()) {
			return List.of();
		}
		ServerLevel level = owner.serverLevel();
		List<Integer> ids = new ArrayList<>(set.size());
		for (UUID bearerId : set) {
			Entity bearer = level.getEntity(bearerId);
			if (bearer != null) {
				ids.add(bearer.getId());
			}
		}
		Collections.sort(ids);
		return ids;
	}

	private static Set<UUID> hearts(UUID ownerId) {
		Set<UUID> set = HEARTS.get(ownerId);
		if (set == null) {
			set = new HashSet<>();
			HEARTS.put(ownerId, ownerId, set);
		}
		return set;
	}

	private static SyncState syncState(UUID ownerId) {
		SyncState sync = SYNC.get(ownerId);
		if (sync == null) {
			sync = new SyncState();
			SYNC.put(ownerId, ownerId, sync);
		}
		return sync;
	}

	/**
	 * Live heart count → {@code ATTACK_DAMAGE} multiplier. Transient and recomputed on
	 * every set mutation (and on passive apply/remove) so the bonus tracks the hearts
	 * the player actually holds right now.
	 */
	public static void refreshDamageScale(Player owner) {
		AttributeInstance attack = owner.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack == null) {
			return;
		}
		double amount = damageScale(owner.getUUID()) - 1.0;
		if (amount <= 0.0) {
			attack.removeModifier(HEARTS_DAMAGE_MODIFIER_ID);
			return;
		}
		attack.addOrUpdateTransientModifier(new AttributeModifier(HEARTS_DAMAGE_MODIFIER_ID,
				amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static float damageScale(UUID ownerId) {
		return 1.0f + 0.02f * count(ownerId);
	}

	public static void clearDamageScale(Player owner) {
		AttributeInstance attack = owner.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack != null) {
			attack.removeModifier(HEARTS_DAMAGE_MODIFIER_ID);
		}
	}
}
