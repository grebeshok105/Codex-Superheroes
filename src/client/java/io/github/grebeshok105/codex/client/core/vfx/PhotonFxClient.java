package io.github.grebeshok105.codex.client.core.vfx;

import com.lowdragmc.photon.client.fx.BlockEffectExecutor;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor.AutoRotate;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXEffectExecutor;
import com.lowdragmc.photon.client.fx.FXHelper;
import io.github.grebeshok105.codex.core.net.PhotonFxS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/**
 * Client mirror of {@link io.github.grebeshok105.codex.core.net.PhotonFx}: resolves the
 * authored {@code .fx} through {@link FXHelper#getFX} and starts the matching Photon
 * executor. A missing fx (not shipped, not in the resource path) is a silent no-op — the
 * VFX lane must never crash gameplay for a cosmetic lookup.
 */
public final class PhotonFxClient {
	private PhotonFxClient() {
	}

	public static void spawn(PhotonFxS2CPayload payload) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			return;
		}
		FX fx = FXHelper.getFX(payload.fx());
		if (fx == null) {
			return;
		}
		Vector3f offset = new Vector3f((float) payload.offset().x, (float) payload.offset().y,
				(float) payload.offset().z);
		Vector3f scale = new Vector3f(payload.scale(), payload.scale(), payload.scale());
		FXEffectExecutor executor;
		if (payload.anchorEntityId() != PhotonFxS2CPayload.NO_ANCHOR) {
			Entity anchor = level.getEntity(payload.anchorEntityId());
			if (anchor == null) {
				return;
			}
			AutoRotate[] modes = AutoRotate.values();
			AutoRotate rotate = payload.rotation() >= 0 && payload.rotation() < modes.length
					? modes[payload.rotation()] : AutoRotate.NONE;
			EntityEffectExecutor entityExec = new EntityEffectExecutor(fx, level, anchor, rotate);
			entityExec.setOffset(offset);
			executor = entityExec;
		} else {
			BlockPos pos = BlockPos.containing(payload.pos());
			BlockEffectExecutor blockExec = new BlockEffectExecutor(fx, level, pos);
			// Photon anchors block executors at pos+0.5; fold the fractional part of pos into
			// the offset so callers can aim at sub-block precision.
			offset = new Vector3f(
					(float) (payload.pos().x - pos.getX() - 0.5 + offset.x),
					(float) (payload.pos().y - pos.getY() - 0.5 + offset.y),
					(float) (payload.pos().z - pos.getZ() - 0.5 + offset.z));
			blockExec.setOffset(offset);
			executor = blockExec;
		}
		executor.setScale(scale);
		executor.setDelay(payload.delay());
		executor.setAllowMulti(payload.allowMulti());
		executor.start();
	}
}
