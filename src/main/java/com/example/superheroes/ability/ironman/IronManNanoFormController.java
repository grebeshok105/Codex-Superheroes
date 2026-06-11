package com.example.superheroes.ability.ironman;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.HeroAttributes;
import com.example.superheroes.network.NanoFormS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Серверная логика нано-форм: применение/снятие атрибутов формы
 * и синхронизация активной формы со всеми клиентами (рендер на руке).
 */
public final class IronManNanoFormController {
	private IronManNanoFormController() {
	}

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer joining = handler.getPlayer();
			for (ServerPlayer other : server.getPlayerList().getPlayers()) {
				int form = other.getAttachedOrCreate(ModAttachments.NANO_FORM);
				if (form != 0) {
					ServerPlayNetworking.send(joining, new NanoFormS2CPayload(other.getUUID(), form));
				}
			}
			broadcast(joining);
		});
	}

	public static IronManNanoForm formOf(ServerPlayer player) {
		return IronManNanoForm.byIndex(player.getAttachedOrCreate(ModAttachments.NANO_FORM));
	}

	public static void setForm(ServerPlayer player, IronManNanoForm form) {
		IronManNanoForm previous = formOf(player);
		removeFormAttributes(player, previous);
		player.setAttached(ModAttachments.NANO_FORM, form.index());
		applyFormAttributes(player, form);
		broadcast(player);
	}

	public static void clear(ServerPlayer player) {
		IronManNanoForm previous = formOf(player);
		if (previous == IronManNanoForm.NONE) {
			return;
		}
		removeFormAttributes(player, previous);
		player.setAttached(ModAttachments.NANO_FORM, 0);
		broadcast(player);
	}

	/** Лёгкий пассивный визуал активной формы (раз в секунду, чтобы не спамить частицами). */
	public static void serverTick(ServerPlayer player) {
		IronManNanoForm form = formOf(player);
		if (form == IronManNanoForm.NONE || player.tickCount % 20 != 0) {
			return;
		}
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.35, 0.4, 0.35, 0.02);
	}

	private static void applyFormAttributes(ServerPlayer player, IronManNanoForm form) {
		switch (form) {
			case BLADE -> HeroAttributes.NANO_BLADE.apply(player);
			case SHIELD -> HeroAttributes.NANO_SHIELD.apply(player);
			default -> {
			}
		}
	}

	private static void removeFormAttributes(ServerPlayer player, IronManNanoForm form) {
		switch (form) {
			case BLADE -> HeroAttributes.NANO_BLADE.remove(player);
			case SHIELD -> HeroAttributes.NANO_SHIELD.remove(player);
			default -> {
			}
		}
	}

	private static void broadcast(ServerPlayer player) {
		int form = player.getAttachedOrCreate(ModAttachments.NANO_FORM);
		NanoFormS2CPayload payload = new NanoFormS2CPayload(player.getUUID(), form);
		for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(p, payload);
		}
	}
}
