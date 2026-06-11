package com.example.superheroes.ability.ironman;

import com.example.superheroes.ability.Ability;
import com.example.superheroes.ability.AbilityCooldowns;
import com.example.superheroes.ability.AbilityIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Нано-формы Mark 85 (Endgame): одна способность-переключатель циклом
 * клинок → супермолот → щит → выкл. Оружие «вырастает» из брони на руке,
 * со «стеканием» нано-частиц при каждой смене.
 *  — Клинок: +7 урона в ближнем бою.
 *  — Супермолот: урон без бонуса, но дикое пробитие стен и отталкивание (CombatImpactEngine).
 *  — Щит: +12 брони, +6 toughness, +40% knockback resistance.
 */
public final class IronManNanoFormAbility implements Ability {
	private static final int COOLDOWN_TICKS = 15;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.IRON_MAN_NANO_FORM;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 30f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		return !AbilityCooldowns.isOnCooldown(player, getId());
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		IronManNanoForm next = IronManNanoFormController.formOf(player).next();
		IronManNanoFormController.setForm(player, next);

		ServerLevel level = player.serverLevel();
		spawnNanoFlow(level, player, next);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, next == IronManNanoForm.NONE ? 1.7f : 1.35f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.6f, 1.6f);

		player.displayClientMessage(Component.translatable("ability.superheroes.iron_man_nano_form.switch",
				Component.translatable(next.translationKey())), true);

		AbilityCooldowns.setCooldownTicks(player, getId(), COOLDOWN_TICKS);
		return true;
	}

	/** «Стекание» нано-частиц от реактора к руке при смене формы. */
	private static void spawnNanoFlow(ServerLevel level, ServerPlayer player, IronManNanoForm form) {
		Vec3 look = player.getLookAngle();
		Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize()
				.scale(form == IronManNanoForm.SHIELD ? -0.38 : 0.38);
		Vec3 chest = player.position().add(0.0, 1.25, 0.0);
		Vec3 hand = player.position().add(side.x, 0.85, side.z).add(look.x * 0.25, 0.0, look.z * 0.25);
		for (int i = 0; i < 14; i++) {
			double t = i / 13.0;
			double x = chest.x + (hand.x - chest.x) * t;
			double y = chest.y + (hand.y - chest.y) * t;
			double z = chest.z + (hand.z - chest.z) * t;
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.04, 0.04, 0.04, 0.01);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.WAX_OFF, x, y, z, 1, 0.03, 0.03, 0.03, 0.0);
			}
		}
		level.sendParticles(ParticleTypes.FLASH, hand.x, hand.y, hand.z, 1, 0.0, 0.0, 0.0, 0.0);
	}
}
