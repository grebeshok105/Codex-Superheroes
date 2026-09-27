package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.AbilityScopedModifiers;
import io.github.grebeshok105.codex.hero.ThanosHero;
import io.github.grebeshok105.codex.item.InfinityGauntletItem;
import io.github.grebeshok105.codex.item.infinity.InfinityGauntletData;
import io.github.grebeshok105.codex.item.infinity.InfinityStoneType;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.network.ThanosStonesS2CPayload;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class ThanosGauntletStateController {
	// Not ClearOn.HERO_CLEAR: a non-Thanos entry must survive untransform so tickPlayer's
	// swap path sees it, strips the stone modifiers and broadcasts an empty set.
	private static final OwnedSessionMap<UUID, EnumSet<InfinityStoneType>> APPLIED =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH));

	private ThanosGauntletStateController() {
	}

	public static void register(HeroModuleContext ctx) {

		// Respawn hook — the fresh entity needs APPLIED reset for the next 10-tick
		// scan to re-apply the transient stone modifiers (leave/death drop it via ClearOn).
		ctx.lifecycle().onRespawn(ThanosGauntletStateController::onPlayerReset);

		ctx.ticks().start(server -> {
			for (Iterator<Map.Entry<UUID, EnumSet<InfinityStoneType>>> it = APPLIED.iterator(); it.hasNext();) {
				if (server.getPlayerList().getPlayer(it.next().getKey()) == null) {
					it.remove();
				}
			}
		});

		// Joining players get every Thanos' current stone set so remote skins render correctly
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			for (Map.Entry<UUID, EnumSet<InfinityStoneType>> entry : APPLIED) {
				ServerPlayNetworking.send(handler.getPlayer(), new ThanosStonesS2CPayload(entry.getKey(), maskOf(entry.getValue())));
			}
		});
	}

	public static void onPlayerReset(ServerPlayer player) {
		APPLIED.remove(player.getUUID());
	}

	public static void sendStones(ServerPlayer player, Set<InfinityStoneType> stones) {
		ThanosStonesS2CPayload payload = new ThanosStonesS2CPayload(player.getUUID(), maskOf(stones));
		for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(p, payload);
		}
	}

	private static int maskOf(Set<InfinityStoneType> stones) {
		int mask = 0;
		for (InfinityStoneType t : stones) {
			mask |= (1 << t.ordinal());
		}
		return mask;
	}

	public static EnumSet<InfinityStoneType> getCurrentStones(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		if (!ThanosHero.ID.equals(data.heroId())) {
			return EnumSet.noneOf(InfinityStoneType.class);
		}
		return scan(player);
	}

	private static EnumSet<InfinityStoneType> scan(ServerPlayer player) {
		EnumSet<InfinityStoneType> out = EnumSet.noneOf(InfinityStoneType.class);
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.getItem() instanceof InfinityGauntletItem) {
				out.addAll(InfinityGauntletData.getStones(stack));
				return out;
			}
		}
		return out;
	}

	private static void applyDelta(ServerPlayer player, Set<InfinityStoneType> applied, Set<InfinityStoneType> wanted) {
		for (InfinityStoneType t : applied) {
			if (!wanted.contains(t)) {
				AttributeInstance instance = player.getAttribute(t.getAttribute());
				if (instance != null) {
					instance.removeModifier(t.getModifierId());
				}
			}
		}
		for (InfinityStoneType t : wanted) {
			if (!applied.contains(t)) {
				AttributeInstance instance = player.getAttribute(t.getAttribute());
				if (instance != null) {
					instance.addOrUpdateTransientModifier(new AttributeModifier(t.getModifierId(), t.getAmount(), t.getOperation()));
				}
			}
		}
	}

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		if (server.getTickCount() % 10 != 0) return;
		if (!ThanosHero.ID.equals(data.heroId())) {
			if (APPLIED.remove(player.getUUID()) != null) {
				AbilityScopedModifiers.thanosClearStoneModifiers(player);
				sendStones(player, EnumSet.noneOf(InfinityStoneType.class));
			}
			return;
		}
		EnumSet<InfinityStoneType> wanted = scan(player);
		EnumSet<InfinityStoneType> applied = APPLIED.get(player.getUUID());
		if (applied == null) {
			applied = EnumSet.noneOf(InfinityStoneType.class);
			APPLIED.put(player.getUUID(), player.getUUID(), applied);
		}
		if (!wanted.equals(applied)) {
			applyDelta(player, applied, wanted);
			applied.clear();
			applied.addAll(wanted);
			sendStones(player, wanted);
		}
	}

}
