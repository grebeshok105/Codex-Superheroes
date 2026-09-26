package io.github.grebeshok105.codex.ability;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Shadow Extraction — поглощение «эссенции» павшего:
 *  +2 HP, +5 ENERGY (Shadow Charges).
 *  - Cost: 10 ENERGY (по дизайну +15 charges, но при стандартной логике
 *    проще: дешёвое нажатие, на возврат бонус-HP).
 *  - CD: 100t (5с)
 */
public final class ShadowExtractionAbility implements Ability {
	private static final int COOLDOWN_TICKS = 100;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.SHADOW_EXTRACTION;
	}

	@Override
	public boolean isToggle() {
		return false;
	}

	@Override
	public float costOnActivate() {
		return 10f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		player.heal(2.0f);

		// Возвращаем 5 ENERGY (charges) — net cost 5
		HeroDataStore.update(player, d -> {
			var hero = io.github.grebeshok105.codex.core.hero.Heroes.get(d.heroId());
			return d.withEnergy(Math.min(d.energy() + 5f, hero != null ? hero.getEnergyMax() : 100f));
		});

		level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(),
				24, 0.4, 0.6, 0.4, 0.05);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7f, 1.4f);

		AbilityCooldowns.setCooldownTicks(player, AbilityIds.SHADOW_EXTRACTION, COOLDOWN_TICKS);
		return true;
	}
}
