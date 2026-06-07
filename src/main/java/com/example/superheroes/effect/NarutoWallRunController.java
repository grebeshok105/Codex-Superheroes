package com.example.superheroes.effect;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.hero.NarutoHero;
import com.example.superheroes.transform.HeroData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class NarutoWallRunController {
	private NarutoWallRunController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
				if (!data.hasHero() || !NarutoHero.ID.equals(data.heroId())) {
					continue;
				}
				if (!player.horizontalCollision || !player.isSprinting() || player.isCrouching()) {
					continue;
				}
				Vec3 delta = player.getDeltaMovement();
				if (delta.y < 0.18) {
					player.setDeltaMovement(delta.x * 1.03, 0.18, delta.z * 1.03);
					player.hurtMarked = true;
				}
			}
		});
	}
}
