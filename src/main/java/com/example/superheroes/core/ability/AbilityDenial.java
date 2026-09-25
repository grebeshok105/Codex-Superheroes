package com.example.superheroes.core.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Why an activation was refused; {@link #SILENT} refuses without telling the player. */
public record AbilityDenial(@Nullable Component message) {
	public static final AbilityDenial SILENT = new AbilityDenial(null);

	public static AbilityDenial of(Component message) {
		return new AbilityDenial(message);
	}

	public void notify(ServerPlayer player) {
		if (message != null) {
			player.displayClientMessage(message, true);
		}
	}
}
