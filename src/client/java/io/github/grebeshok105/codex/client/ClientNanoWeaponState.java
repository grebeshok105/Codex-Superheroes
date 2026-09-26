package io.github.grebeshok105.codex.client;

import io.github.grebeshok105.codex.ability.AbilityIds;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Клиентский выбор «нано-оружия» Железного Человека для HUD-стрипа снизу.
 * Чисто визуальный/навигационный селектор активного орудия (репульсор / ракеты /
 * унибим). Переключается клавишей NANO_WEAPON.
 */
public final class ClientNanoWeaponState {
	public record Weapon(ResourceLocation abilityId, String label) {
	}

	public static final List<Weapon> WEAPONS = List.of(
			new Weapon(AbilityIds.REPULSOR, "REPULSOR"),
			new Weapon(AbilityIds.IRON_MAN_SMART_MISSILE, "MISSILES"),
			new Weapon(AbilityIds.UNIBEAM, "UNIBEAM")
	);

	private static int selected = 0;
	private static long lastSwitchMs = 0L;

	static {
		ClientSessionState.register(ClientNanoWeaponState::reset);
	}

	private ClientNanoWeaponState() {
	}

	public static int selectedIndex() {
		return selected;
	}

	public static Weapon selected() {
		return WEAPONS.get(selected);
	}

	public static long lastSwitchMs() {
		return lastSwitchMs;
	}

	public static void cycle(int dir) {
		int n = WEAPONS.size();
		selected = ((selected + dir) % n + n) % n;
		lastSwitchMs = System.currentTimeMillis();
	}

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		selected = 0;
		lastSwitchMs = 0L;
	}
}
