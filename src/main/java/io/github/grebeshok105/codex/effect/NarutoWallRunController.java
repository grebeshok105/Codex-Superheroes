package io.github.grebeshok105.codex.effect;

import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.hero.NarutoHero;
import io.github.grebeshok105.codex.transform.HeroData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.MinecraftServer;

public final class NarutoWallRunController {
	private NarutoWallRunController() {
	}


	public static void tickPlayer(MinecraftServer server, ServerPlayer player, HeroData data) {
		if (!data.hasHero() || !NarutoHero.ID.equals(data.heroId())) {
			return;
		}
		if (!player.horizontalCollision || !player.isSprinting() || player.isCrouching()) {
			return;
		}
		Vec3 delta = player.getDeltaMovement();
		if (delta.y < 0.18) {
			player.setDeltaMovement(delta.x * 1.03, 0.18, delta.z * 1.03);
			player.hurtMarked = true;
		}
	}

}
