package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.network.ReinhardSwordKillS2CPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Когда меч Рейнхарда наносит игроку летальный удар, мы:
 *  - отменяем смерть, садим жертву на 1 ХП,
 *  - запоминаем её и кто её отметил,
 *  - на её клиент шлём S2C payload — там HUD рисует кровавый оверлей и текст «Сражён мечом Рейнхарда»,
 *  - после окончания time-slow {@link ReinhardTimeSlowController} вызывает {@link #flushDeaths(MinecraftServer)},
 *    и тут жертва добивается финальным ударом.
 *
 * На обычных мобах НЕ работает (та же ALLOW_DAMAGE listener фильтрует instanceof ServerPlayer).
 */
public final class ReinhardSwordDeathMarkController {
	/** victim UUID -> attacker UUID. Victim leaving or dying drops the mark (audit B17:
	 * a mark must never execute on a respawned or relogged player). */
	private static final OwnedSessionMap<UUID, UUID> MARKED =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH));
	private static final OwnedSessionMap<UUID, Boolean> FINALIZING =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH));
	private static final OwnedSessionMap<UUID, Boolean> BYPASS =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH));

	private ReinhardSwordDeathMarkController() {
	}

	public static void register(HeroModuleContext ctx) {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer victim)) return true;
			if (BYPASS.containsKey(victim.getUUID())) return true;
			if (FINALIZING.containsKey(victim.getUUID())) return true;
			if (!(source.getEntity() instanceof ServerPlayer attacker)) return true;
			if (attacker == victim) return true;
			if (!ReinhardController.isReinhard(attacker)) return true;
			if (!source.is(DamageTypes.PLAYER_ATTACK)) return true;
			if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			if (!(attacker.getMainHandItem().getItem() instanceof io.github.grebeshok105.codex.item.RoyalIcicleItem)) return true;
			ReinhardState astate = attacker.getAttachedOrCreate(ModAttachments.REINHARD_STATE);
			if (!astate.swordDrawn()) return true;
			if (!ReinhardTimeSlowController.isActive(attacker)) return true;
			if (!MARKED.containsKey(victim.getUUID())) {
				MARKED.put(victim.getUUID(), victim.getUUID(), attacker.getUUID());
			}
			victim.setAbsorptionAmount(0.0f);
			victim.setHealth(1.0f);
			victim.invulnerableTime = 0;
			return false;
		});
	}

	public static boolean hurtBypassingMark(ServerPlayer victim, net.minecraft.world.damagesource.DamageSource source, float amount) {
		BYPASS.put(victim.getUUID(), victim.getUUID(), Boolean.TRUE);
		try {
			return victim.hurt(source, amount);
		} finally {
			BYPASS.remove(victim.getUUID());
		}
	}

	public static void flushDeaths(MinecraftServer server) {
		if (MARKED.size() == 0) return;
		Iterator<Map.Entry<UUID, UUID>> it = MARKED.iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, UUID> entry = it.next();
			it.remove();
			ServerPlayer victim = server.getPlayerList().getPlayer(entry.getKey());
			if (victim == null) continue;
			ServerPlayNetworking.send(victim, new ReinhardSwordKillS2CPayload(false));
			ServerPlayer attacker = server.getPlayerList().getPlayer(entry.getValue());
			var damage = attacker != null
					? victim.serverLevel().damageSources().playerAttack(attacker)
					: victim.serverLevel().damageSources().genericKill();
			victim.invulnerableTime = 0;
			FINALIZING.put(victim.getUUID(), victim.getUUID(), Boolean.TRUE);
			try {
				victim.hurt(damage, Float.MAX_VALUE);
			} finally {
				FINALIZING.remove(victim.getUUID());
			}
		}
	}
}
