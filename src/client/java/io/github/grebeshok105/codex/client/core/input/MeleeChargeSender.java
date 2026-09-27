package io.github.grebeshok105.codex.client.core.input;

import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.client.ClientMeleeChargeState;
import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.core.net.HeroMeleeChargeC2SPayload;
import io.github.grebeshok105.codex.mechanic.boundweapon.BoundWeaponItem;
import io.github.grebeshok105.codex.mechanic.impact.ImpactChargeRules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;

/**
 * Charged-melee input state machine: watches the use key on START_CLIENT_TICK and emits
 * {@link HeroMeleeChargeC2SPayload} START / RELEASE / CANCEL while mirroring charge ticks
 * into {@link ClientMeleeChargeState} for the HUD.
 */
public final class MeleeChargeSender {
	private static final class State {
		boolean sent;
		int ticks;
	}

	private static final State STATE = new State();

	static {
		ClientSessionState.register(MeleeChargeSender::reset);
	}

	private MeleeChargeSender() {
	}

	public static void init() {
		ClientTickEvents.START_CLIENT_TICK.register(MeleeChargeSender::tick);
	}

	private static void reset() {
		STATE.sent = false;
		STATE.ticks = 0;
	}

	private static void tick(Minecraft client) {
		boolean shouldCharge = client.player != null
				&& client.level != null
				&& client.screen == null
				&& ClientHeroState.data().hasHero()
				&& client.options.keyUse.isDown()
				&& canChargeWithHands(client.player);
		if (shouldCharge && !STATE.sent) {
			ClientPlayNetworking.send(new HeroMeleeChargeC2SPayload(HeroMeleeChargeC2SPayload.ACTION_START, 0, -1));
			STATE.sent = true;
			STATE.ticks = 0;
			ClientMeleeChargeState.update(true, 0);
			return;
		}
		if (shouldCharge) {
			STATE.ticks = Math.min(ImpactChargeRules.CAP_TICKS, STATE.ticks + 1);
			ClientMeleeChargeState.update(true, STATE.ticks);
			return;
		}
		if (STATE.sent) {
			boolean release = client.player != null
					&& client.level != null
					&& client.screen == null
					&& ClientHeroState.data().hasHero();
			int action = release ? HeroMeleeChargeC2SPayload.ACTION_RELEASE : HeroMeleeChargeC2SPayload.ACTION_CANCEL;
			ClientPlayNetworking.send(new HeroMeleeChargeC2SPayload(action, STATE.ticks, hoveredEntityId(client)));
			STATE.sent = false;
			STATE.ticks = 0;
		}
		ClientMeleeChargeState.clearAll();
	}

	private static boolean canChargeWithHands(Player player) {
		if (player.isUsingItem()) {
			return false;
		}
		// Заряженные удары только на пустых кулаках: предмет в основной руке полностью отключает тиры.
		return player.getMainHandItem().isEmpty() && chargeFriendly(player.getOffhandItem());
	}

	private static boolean chargeFriendly(ItemStack stack) {
		if (stack.isEmpty()) {
			return true;
		}
		if (stack.getItem() instanceof BlockItem
				|| (stack.getItem() instanceof BoundWeaponItem boundWeapon && boundWeapon.blocksChargedAttackTiers())) {
			return false;
		}
		return stack.getUseAnimation() == UseAnim.NONE;
	}

	private static int hoveredEntityId(Minecraft client) {
		if (client.hitResult != null && client.hitResult.getType() == Type.ENTITY
				&& client.hitResult instanceof EntityHitResult entityHit) {
			return entityHit.getEntity().getId();
		}
		return -1;
	}
}
