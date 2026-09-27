package io.github.grebeshok105.codex.hero.homelander.runtime;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import io.github.grebeshok105.codex.hero.homelander.item.UraniumIsotopeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.UUID;
import io.github.grebeshok105.codex.core.model.HeroData;
import net.minecraft.server.MinecraftServer;

public final class UraniumOffhandController {
	private static final ResourceLocation KB_MODIFIER_ID = ModId.of("uranium_offhand_kb");
	private static final double KB_AMOUNT = 0.5;
	private static final int RAD_TICK_PER_STACK = 100;
	private static final int RAD_MAX_STACKS = 5;

	// ClearOn.LEAVE replaces the offline-player sweep pruneGonePlayers ran every tick.
	private static final OwnedSessionMap<UUID, Integer> radiationTicks =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE));

	private UraniumOffhandController() {
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

	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		ItemStack offhand = player.getOffhandItem();
		boolean holding = offhand.getItem() instanceof UraniumIsotopeItem;
		if (holding) {
			applyKbResistance(player);
			UUID id = player.getUUID();
			Integer prev = radiationTicks.get(id);
			int ticks = prev == null ? 1 : prev + 1;
			radiationTicks.put(id, id, ticks);
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

}
