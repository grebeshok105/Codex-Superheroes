package com.example.superheroes.effect;

import com.example.superheroes.ModId;
import com.example.superheroes.item.UraniumIsotopeItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class UraniumOffhandController {
	private static final ResourceLocation KB_MODIFIER_ID = ModId.of("uranium_offhand_kb");
	private static final double KB_AMOUNT = 0.5;
	private static final int RAD_TICK_PER_STACK = 100;
	private static final int RAD_MAX_STACKS = 5;

	private static final Map<UUID, Integer> radiationTicks = new HashMap<>();

	private UraniumOffhandController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				ItemStack offhand = player.getOffhandItem();
				boolean holding = offhand.getItem() instanceof UraniumIsotopeItem;
				if (holding) {
					applyKbResistance(player);
					int ticks = radiationTicks.merge(player.getUUID(), 1, Integer::sum);
					int stacks = Math.min(RAD_MAX_STACKS, ticks / RAD_TICK_PER_STACK);
					if (stacks >= RAD_MAX_STACKS) {
						player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1, false, true, true));
						player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, false, true, true));
					} else if (stacks >= 3) {
						player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, false, true, true));
					}
				} else {
					removeKbResistance(player);
					radiationTicks.remove(player.getUUID());
				}
			}
			Iterator<UUID> it = radiationTicks.keySet().iterator();
			while (it.hasNext()) {
				UUID id = it.next();
				if (server.getPlayerList().getPlayer(id) == null) it.remove();
			}
		});
	}

	private static void applyKbResistance(ServerPlayer player) {
		AttributeInstance attr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (attr == null) return;
		if (attr.getModifier(KB_MODIFIER_ID) != null) return;
		attr.addTransientModifier(new AttributeModifier(KB_MODIFIER_ID, KB_AMOUNT, AttributeModifier.Operation.ADD_VALUE));
	}

	private static void removeKbResistance(ServerPlayer player) {
		AttributeInstance attr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (attr == null) return;
		attr.removeModifier(KB_MODIFIER_ID);
	}
}
