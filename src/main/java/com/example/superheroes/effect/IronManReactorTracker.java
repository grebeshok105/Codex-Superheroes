package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.IronManHero;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.network.ReactorStateS2CPayload;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class IronManReactorTracker {
	private static final float TRIGGER_THRESHOLD = 100f; // <10% of 1000
	private static final float REFILL_AMOUNT = 500f;     // restore to soft-cap
	private static final int REPLACE_TICKS = 100;        // 5s standstill
	private static final double STILL_TOLERANCE_SQ = 0.0025; // ~0.05 block movement

	private static final Map<UUID, State> states = new HashMap<>();

	private static final class State {
		boolean active;
		int progress;
		Vec3 anchor;
		boolean hasStock;
	}

	private IronManReactorTracker() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player);
			}
		});
	}

	public static boolean isReplacing(ServerPlayer player) {
		State s = states.get(player.getUUID());
		return s != null && s.active;
	}

	private static void tick(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		UUID id = player.getUUID();
		if (!data.hasHero() || !IronManHero.ID.equals(data.heroId()) || !player.isAlive()) {
			if (states.remove(id) != null) {
				sendState(player, false, 0, false);
			}
			return;
		}

		State s = states.get(id);
		float energy = data.energy();

		if (s == null) {
			if (energy < TRIGGER_THRESHOLD) {
				s = new State();
				s.active = true;
				s.progress = 0;
				s.anchor = player.position();
				s.hasStock = playerHasReactor(player);
				states.put(id, s);
				ServerLevel level = player.serverLevel();
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 0.7f);
			} else {
				return;
			}
		}

		s.hasStock = playerHasReactor(player);

		anchorPlayer(player, s.anchor);

		HeroData zeroed = data.withResources(0f, data.mana());
		player.setAttached(ModAttachments.HERO_DATA, zeroed);

		Vec3 cur = player.position();
		double dx = cur.x - s.anchor.x;
		double dz = cur.z - s.anchor.z;
		double horiz = dx * dx + dz * dz;
		if (horiz > STILL_TOLERANCE_SQ) {
			s.progress = 0;
			s.anchor = cur;
		}

		if (!s.hasStock) {
			s.progress = 0;
			sendState(player, true, 0, false);
			emitDistress(player);
			return;
		}

		s.progress++;

		if (player.tickCount % 4 == 0) {
			ServerLevel level = player.serverLevel();
			Vec3 chest = cur.add(0, 1.0, 0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					chest.x, chest.y, chest.z, 6, 0.3, 0.2, 0.3, 0.0);
			level.sendParticles(ParticleTypes.END_ROD,
					chest.x, chest.y, chest.z, 2, 0.2, 0.1, 0.2, 0.005);
			if (player.tickCount % 12 == 0) {
				level.playSound(null, cur.x, cur.y, cur.z,
						SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.6f);
			}
		}

		sendState(player, true, s.progress, true);

		if (s.progress >= REPLACE_TICKS) {
			finishReplace(player, zeroed);
			states.remove(id);
		}
	}

	private static void anchorPlayer(ServerPlayer player, Vec3 anchor) {
		Vec3 cur = player.position();
		double dy = Math.abs(cur.y - anchor.y);
		if (dy > 1.0) {
			player.connection.teleport(anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
		}
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0f;
		player.hurtMarked = true;
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 250, false, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, 5, 128, false, false, false));
	}

	private static boolean playerHasReactor(ServerPlayer player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty() && stack.is(ModItems.IRON_MAN_REACTOR)) {
				return true;
			}
		}
		return false;
	}

	private static int findReactorSlot(ServerPlayer player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty() && stack.is(ModItems.IRON_MAN_REACTOR)) {
				return i;
			}
		}
		return -1;
	}

	private static void finishReplace(ServerPlayer player, HeroData currentData) {
		int slot = findReactorSlot(player);
		if (slot < 0) {
			sendState(player, true, 0, false);
			return;
		}
		ItemStack stack = player.getInventory().getItem(slot);
		stack.shrink(1);
		player.getInventory().setChanged();

		HeroData refilled = currentData.withResources(REFILL_AMOUNT, currentData.mana());
		player.setAttached(ModAttachments.HERO_DATA, refilled);
		com.example.superheroes.network.ModNetworking.syncResources(player, refilled);

		ServerLevel level = player.serverLevel();
		Vec3 cur = player.position();
		level.sendParticles(ParticleTypes.FLASH,
				cur.x, cur.y + 1.0, cur.z, 3, 0.2, 0.2, 0.2, 0.0);
		level.sendParticles(ParticleTypes.END_ROD,
				cur.x, cur.y + 1.0, cur.z, 24, 0.5, 0.5, 0.5, 0.05);
		level.playSound(null, cur.x, cur.y, cur.z,
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
		level.playSound(null, cur.x, cur.y, cur.z,
				SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.7f, 1.4f);

		sendState(player, false, 0, true);
	}

	private static void emitDistress(ServerPlayer player) {
		if (player.tickCount % 20 != 0) {
			return;
		}
		ServerLevel level = player.serverLevel();
		Vec3 cur = player.position();
		level.sendParticles(ParticleTypes.SMOKE,
				cur.x, cur.y + 1.0, cur.z, 8, 0.3, 0.3, 0.3, 0.02);
		level.playSound(null, cur.x, cur.y, cur.z,
				SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.5f, 0.8f);
	}

	private static void sendState(ServerPlayer player, boolean active, int progress, boolean hasStock) {
		ServerPlayNetworking.send(player, new ReactorStateS2CPayload(active, progress, REPLACE_TICKS, hasStock));
	}
}
