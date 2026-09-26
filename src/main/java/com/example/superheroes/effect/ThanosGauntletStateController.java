package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.AbilityScopedModifiers;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.item.InfinityGauntletItem;
import com.example.superheroes.item.infinity.InfinityGauntletData;
import com.example.superheroes.item.infinity.InfinityStoneType;
import com.example.superheroes.network.ThanosStonesS2CPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class ThanosGauntletStateController {
	private static final Map<UUID, EnumSet<InfinityStoneType>> APPLIED = new HashMap<>();

	private ThanosGauntletStateController() {
	}

	public static void register(HeroModuleContext ctx) {

		ctx.ticks().start(server -> {
			APPLIED.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
		});

		// Joining players get every Thanos' current stone set so remote skins render correctly
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			for (Map.Entry<UUID, EnumSet<InfinityStoneType>> entry : APPLIED.entrySet()) {
				ServerPlayNetworking.send(handler.getPlayer(), new ThanosStonesS2CPayload(entry.getKey(), maskOf(entry.getValue())));
			}
		});
	}

	/**
	 * Death/respawn hook — stone buffs are transient modifiers now, so the fresh entity needs
	 * {@link #APPLIED} reset for the next 10-tick scan to re-apply them (the old permanent
	 * modifiers died with the entity anyway, but {@code APPLIED} wrongly claimed they remained).
	 */
	public static void onPlayerReset(ServerPlayer player) {
		APPLIED.remove(player.getUUID());
	}

	/** World shutdown — applied-set tracking dies with the world. */
	public static void resetAll() {
		APPLIED.clear();
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
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
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
		EnumSet<InfinityStoneType> applied = APPLIED.computeIfAbsent(player.getUUID(), id -> EnumSet.noneOf(InfinityStoneType.class));
		if (!wanted.equals(applied)) {
			applyDelta(player, applied, wanted);
			applied.clear();
			applied.addAll(wanted);
			sendStones(player, wanted);
		}
	}

}
