package com.example.superheroes.effect;

import com.example.superheroes.combat.TargetFilters;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.lifecycle.ControlLockKind;
import com.example.superheroes.lifecycle.EntityControlLock;
import com.example.superheroes.network.ReinhardTimeSlowS2CPayload;
import com.example.superheroes.sound.ModSounds;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reinhard's time slow: замораживает сущностей в радиусе вокруг Reinhard'а,
 * а не весь сервер через {@code tickRateManager} (аудит B11). Мобы получают
 * NoAI/NoGravity через {@link EntityControlLock} и обнуляют скорость; игроки
 * получают transient-модификатор MOVEMENT_SPEED/JUMP_STRENGTH до нуля и
 * блокировку атак/использования предметов через Fabric callbacks.
 */
public final class ReinhardTimeSlowController {
	private static final int SLOW_DURATION_TICKS = 170;
	private static final double FREEZE_RADIUS = 80.0;
	private static final ResourceLocation FREEZE_MODIFIER_ID = com.example.superheroes.ModId.of("time_slow_freeze");

	private record ActiveSlow(long endTick, ResourceKey<Level> level, Set<UUID> frozenEntities) {
	}

	private static final Set<UUID> ARMED = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, ActiveSlow> ACTIVE = new ConcurrentHashMap<>();
	private static final Set<UUID> FROZEN_PLAYERS = ConcurrentHashMap.newKeySet();

	private ReinhardTimeSlowController() {
	}

	public static void register(HeroModuleContext ctx) {

		// Триггер ТОЛЬКО от ручного ЛКМ (AttackEntityCallback), а не от любого источника урона.
		// Контратаки/риспосты/AoE-абилки больше не активируют замедление.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide()) return InteractionResult.PASS;
			if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			if (!(player instanceof ServerPlayer attacker)) return InteractionResult.PASS;
			if (isFrozen(attacker)) return InteractionResult.FAIL;
			if (!(entity instanceof LivingEntity living) || living == attacker) return InteractionResult.PASS;
			if (!living.isAlive()) return InteractionResult.PASS;
			if (!ReinhardController.isReinhard(attacker)) return InteractionResult.PASS;
			if (!ARMED.contains(attacker.getUUID())) return InteractionResult.PASS;
			if (!(attacker.getMainHandItem().getItem() instanceof com.example.superheroes.item.RoyalIcicleItem)) return InteractionResult.PASS;
			ReinhardState rstate = attacker.getAttachedOrCreate(com.example.superheroes.attachment.ModAttachments.REINHARD_STATE);
			if (!rstate.swordDrawn()) return InteractionResult.PASS;
			if (!ARMED.remove(attacker.getUUID())) return InteractionResult.PASS;
			triggerSlow(attacker);
			return InteractionResult.PASS;
		});

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
				(player instanceof ServerPlayer sp && isFrozen(sp)) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseItemCallback.EVENT.register((player, world, hand) ->
				(player instanceof ServerPlayer sp && isFrozen(sp))
						? InteractionResultHolder.fail(player.getItemInHand(hand))
						: InteractionResultHolder.pass(player.getItemInHand(hand)));
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
				(player instanceof ServerPlayer sp && isFrozen(sp)) ? InteractionResult.FAIL : InteractionResult.PASS);

		// Not OwnedSessionMap: entries carry release obligations — ACTIVE holds the frozen-entity
		// ids releaseSlow() must unlock, so the drops stay inside the explicit hooks.
		ctx.lifecycle().onLeave(ReinhardTimeSlowController::onPlayerGone);
		ctx.lifecycle().onDeath(ReinhardTimeSlowController::onPlayerGone);
		ctx.lifecycle().onServerStopped(ReinhardTimeSlowController::resetAll);
	}

	public static void armForFirstStrike(ServerPlayer player) {
		ARMED.add(player.getUUID());
	}

	public static void disarmForFirstStrike(ServerPlayer player) {
		ARMED.remove(player.getUUID());
	}

	public static boolean isActive(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	public static boolean isFrozen(ServerPlayer player) {
		return FROZEN_PLAYERS.contains(player.getUUID());
	}

	public static void triggerAbilitySlow(ServerPlayer player) {
		triggerSlow(player);
	}

	private static void triggerSlow(ServerPlayer player) {
		long endTick = player.level().getGameTime() + SLOW_DURATION_TICKS;
		ACTIVE.put(player.getUUID(), new ActiveSlow(endTick, player.level().dimension(), ConcurrentHashMap.newKeySet()));

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.REINHARD_SWORD_STRIKE_VOICE, SoundSource.PLAYERS, 2.0f, 1.0f);

		broadcastTimeSlow(player, true);
	}

	private static void broadcastTimeSlow(ServerPlayer reinhard, boolean active) {
		ServerLevel level = reinhard.serverLevel();
		AABB box = new AABB(reinhard.position(), reinhard.position()).inflate(FREEZE_RADIUS);
		ReinhardTimeSlowS2CPayload payload = new ReinhardTimeSlowS2CPayload(active);
		for (ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, box, p -> true)) {
			ServerPlayNetworking.send(target, payload);
		}
	}

	private static void broadcastTimeSlowOff(MinecraftServer server) {
		ReinhardTimeSlowS2CPayload payload = new ReinhardTimeSlowS2CPayload(false);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(p, payload);
		}
	}

	public static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		Set<UUID> shouldBeFrozen = ConcurrentHashMap.newKeySet();

		Iterator<Map.Entry<UUID, ActiveSlow>> it = ACTIVE.entrySet().iterator();
		boolean anyEnded = false;
		while (it.hasNext()) {
			Map.Entry<UUID, ActiveSlow> e = it.next();
			ActiveSlow slow = e.getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(e.getKey());
			if (now >= slow.endTick() || owner == null || !owner.isAlive()) {
				releaseSlow(server, e.getKey(), slow);
				it.remove();
				anyEnded = true;
				if (owner != null) {
					rearmIfStillDrawn(owner);
				}
				continue;
			}
			ServerLevel level = server.getLevel(slow.level());
			if (level == null) {
				continue;
			}
			freezeAround(owner, level, slow);
			collectFrozenPlayers(owner, level, slow, shouldBeFrozen);
		}

		syncFrozenPlayers(server, shouldBeFrozen);

		if (anyEnded && ACTIVE.isEmpty()) {
			broadcastTimeSlowOff(server);
			ReinhardSwordDeathMarkController.flushDeaths(server);
		}
	}

	private static void freezeAround(ServerPlayer owner, ServerLevel level, ActiveSlow slow) {
		AABB box = new AABB(owner.position(), owner.position()).inflate(FREEZE_RADIUS);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> TargetFilters.harmableBy(e, owner))) {
			if (entity instanceof ServerPlayer victim) {
				continue; // игроки обрабатываются отдельно через FROZEN_PLAYERS
			}
			if (entity instanceof Mob mob) {
				EntityControlLock.acquire(mob, ControlLockKind.NO_AI, owner);
			}
			EntityControlLock.acquire(entity, ControlLockKind.NO_GRAVITY, owner);
			entity.setDeltaMovement(Vec3.ZERO);
			entity.hurtMarked = true;
			slow.frozenEntities().add(entity.getUUID());
		}
	}

	private static void collectFrozenPlayers(ServerPlayer owner, ServerLevel level, ActiveSlow slow, Set<UUID> out) {
		AABB box = new AABB(owner.position(), owner.position()).inflate(FREEZE_RADIUS);
		for (ServerPlayer victim : level.getEntitiesOfClass(ServerPlayer.class, box,
				p -> TargetFilters.harmableBy(p, owner))) {
			out.add(victim.getUUID());
			victim.setDeltaMovement(Vec3.ZERO);
			victim.hurtMarked = true;
		}
	}

	private static void syncFrozenPlayers(MinecraftServer server, Set<UUID> shouldBeFrozen) {
		Iterator<UUID> it = FROZEN_PLAYERS.iterator();
		while (it.hasNext()) {
			UUID id = it.next();
			if (!shouldBeFrozen.contains(id)) {
				it.remove();
				removeFreezeModifiers(server, id);
			}
		}
		for (UUID id : shouldBeFrozen) {
			if (FROZEN_PLAYERS.add(id)) {
				applyFreezeModifiers(server, id);
			}
		}
	}

	private static void applyFreezeModifiers(MinecraftServer server, UUID playerId) {
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (player == null) return;
		AttributeModifier zero = new AttributeModifier(FREEZE_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.addOrUpdateTransientModifier(zero);
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump != null) jump.addOrUpdateTransientModifier(zero);
	}

	private static void removeFreezeModifiers(MinecraftServer server, UUID playerId) {
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (player == null) return;
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.removeModifier(FREEZE_MODIFIER_ID);
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump != null) jump.removeModifier(FREEZE_MODIFIER_ID);
	}

	private static void releaseSlow(MinecraftServer server, UUID ownerId, ActiveSlow slow) {
		ServerLevel level = server.getLevel(slow.level());
		if (level == null) return;
		for (UUID victimId : slow.frozenEntities()) {
			if (level.getEntity(victimId) instanceof LivingEntity entity) {
				EntityControlLock.release(entity, ControlLockKind.NO_AI, ownerId);
				EntityControlLock.release(entity, ControlLockKind.NO_GRAVITY, ownerId);
			}
		}
	}

	private static void rearmIfStillDrawn(ServerPlayer player) {
		var attach = com.example.superheroes.attachment.ModAttachments.REINHARD_STATE;
		ReinhardState state = player.getAttachedOrCreate(attach);
		if (state.swordDrawn()) {
			ARMED.add(player.getUUID());
		}
	}

	/** Жертва/владелец ушёл или умер: его замедление и принадлежащие ему замки снимаются. */
	public static void onPlayerGone(ServerPlayer player) {
		UUID id = player.getUUID();
		ARMED.remove(id);
		FROZEN_PLAYERS.remove(id);
		ActiveSlow slow = ACTIVE.remove(id);
		MinecraftServer server = player.getServer();
		if (server == null) return;
		removeFreezeModifiers(server, id);
		if (slow != null) {
			releaseSlow(server, id, slow);
		}
	}

	/** World shutdown — все замедления умирают вместе с миром. */
	public static void resetAll(MinecraftServer server) {
		for (Map.Entry<UUID, ActiveSlow> e : ACTIVE.entrySet()) {
			releaseSlow(server, e.getKey(), e.getValue());
		}
		ACTIVE.clear();
		ARMED.clear();
		for (UUID id : FROZEN_PLAYERS) {
			removeFreezeModifiers(server, id);
		}
		FROZEN_PLAYERS.clear();
	}
}
