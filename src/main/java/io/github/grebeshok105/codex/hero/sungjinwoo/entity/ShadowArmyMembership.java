package io.github.grebeshok105.codex.hero.sungjinwoo.entity;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Legitimacy check a shadow soldier runs on itself: is the owner still Sung and
 * still listing this shadow in the army. The runtime impl is wired in by the module
 * so the entity leaf never imports the controller.
 */
@FunctionalInterface
public interface ShadowArmyMembership {
	boolean isMember(ServerPlayer owner, UUID shadowId);
}
