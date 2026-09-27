package io.github.grebeshok105.codex.client.hero.ironman.state;

import io.github.grebeshok105.codex.hero.ironman.runtime.IronManSuitVariant;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.hero.ironman.IronManHero;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.ClientHeroState;

/**
 * Клиентская анимация нано-одевания Mark 50/85.
 *
 * Каждый тик сравнивает «должен ли игрок быть в броне Железного Человека»
 * с прошлым состоянием. На включении запускает сборку (~1.5 c): потоки частиц
 * от реактора и постепенное проявление частей костюма (грудь → руки → ноги → шлем);
 * на снятии — обратную анимацию с возвратом частиц в реактор.
 *
 * Дополнительно: нано-регенерация — после получения урона по броне «пробегают»
 * ремонтные частицы (чистый визуал).
 */
public final class ClientNanoSuitUpState {
	public static final long DURATION_MS = 1500L;
	private static final long REPAIR_MS = 1200L;

	private static final class Maps {
		final Map<UUID, ResourceLocation> lastSuit = new HashMap<>();
		final Map<UUID, Anim> anims = new HashMap<>();
		final Map<UUID, Long> repairs = new HashMap<>();
	}

	private static final Maps MAPS = new Maps();

	static {
		ClientSessionState.register(ClientNanoSuitUpState::reset);
	}

	private ClientNanoSuitUpState() {
	}

	public static void clientTick(Minecraft mc) {
		if (mc.level == null) {
			reset();
			return;
		}
		long now = Util.getMillis();
		for (AbstractClientPlayer player : mc.level.players()) {
			UUID id = player.getUUID();
			ResourceLocation current = currentSuitTexture(player);
			ResourceLocation previous = MAPS.lastSuit.get(id);
			if (current != null && previous == null) {
				MAPS.anims.put(id, new Anim(false, current, now));
			} else if (current == null && previous != null) {
				MAPS.anims.put(id, new Anim(true, previous, now));
			}
			if (current != null) {
				MAPS.lastSuit.put(id, current);
			} else {
				MAPS.lastSuit.remove(id);
			}

			Anim anim = MAPS.anims.get(id);
			if (anim != null) {
				if (now - anim.startMillis > DURATION_MS) {
					MAPS.anims.remove(id);
				} else {
					spawnAssemblyParticles(mc, player, anim, now);
				}
			}

			// Нано-регенерация: hurtTime синхронизируется ванилью, 9 = только что ударили.
			if (current != null && MAPS.anims.get(id) == null && player.hurtTime == 9) {
				MAPS.repairs.put(id, now + REPAIR_MS);
			}
			Long repairEnd = MAPS.repairs.get(id);
			if (repairEnd != null) {
				if (now > repairEnd) {
					MAPS.repairs.remove(id);
				} else if (current != null) {
					spawnRepairParticles(mc, player);
				} else {
					MAPS.repairs.remove(id);
				}
			}
		}
		// Чистка вышедших игроков.
		MAPS.anims.entrySet().removeIf(e -> now - e.getValue().startMillis > DURATION_MS * 2);
		Iterator<UUID> it = MAPS.lastSuit.keySet().iterator();
		while (it.hasNext()) {
			UUID id = it.next();
			boolean online = mc.level.players().stream().anyMatch(p -> p.getUUID().equals(id));
			if (!online) {
				it.remove();
				MAPS.anims.remove(id);
				MAPS.repairs.remove(id);
			}
		}
	}

	/** Пока идёт сборка, геройский скин не подменяется — броня проявляется слоем поверх. */
	public static boolean suppressHeroSkin(UUID playerId) {
		Anim anim = MAPS.anims.get(playerId);
		return anim != null && !anim.disassemble;
	}

	@Nullable
	public static Anim animFor(UUID playerId) {
		return MAPS.anims.get(playerId);
	}

	/** Прогресс сборки 0..1 (для разборки — это прогресс исчезновения). */
	public static float assembledFraction(Anim anim, long nowMillis) {
		float p = Mth.clamp((nowMillis - anim.startMillis) / (float) DURATION_MS, 0f, 1f);
		return anim.disassemble ? 1f - p : p;
	}

	@Nullable
	private static ResourceLocation currentSuitTexture(AbstractClientPlayer player) {
		ResourceLocation heroId;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
			heroId = ClientHeroState.data().hasHero() ? ClientHeroState.data().heroId() : null;
		} else {
			heroId = player.getAttached(io.github.grebeshok105.codex.core.attachment.CoreAttachments.PUBLIC_HERO);
		}
		if (!IronManHero.ID.equals(heroId)) {
			return null;
		}
		int variant = ClientSuitVariantState.variantFor(player.getUUID());
		return IronManSuitVariant.get(variant).texture();
	}

	/** Потоки нано-частиц: реактор → собираемая часть тела (при разборке — наоборот). */
	private static void spawnAssemblyParticles(Minecraft mc, AbstractClientPlayer player, Anim anim, long now) {
		if (mc.level == null) {
			return;
		}
		float f = assembledFraction(anim, now);
		Vec3 reactor = player.position().add(0.0, 1.25, 0.0);
		Vec3 part = partPosition(player, f);
		for (int i = 0; i < 3; i++) {
			double jx = (mc.level.random.nextDouble() - 0.5) * 0.3;
			double jy = (mc.level.random.nextDouble() - 0.5) * 0.3;
			double jz = (mc.level.random.nextDouble() - 0.5) * 0.3;
			Vec3 from = anim.disassemble ? part.add(jx, jy, jz) : reactor;
			Vec3 to = anim.disassemble ? reactor : part.add(jx, jy, jz);
			Vec3 vel = to.subtract(from).scale(0.45);
			mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, vel.x, vel.y, vel.z);
			if (i == 0) {
				mc.level.addParticle(ParticleTypes.WAX_OFF, to.x, to.y, to.z, 0.0, 0.02, 0.0);
			}
		}
	}

	/** Примерная позиция собираемой сейчас части тела (грудь → руки → ноги → шлем). */
	private static Vec3 partPosition(AbstractClientPlayer player, float f) {
		Vec3 base = player.position();
		if (f < 0.3f) {
			return base.add(0.0, 1.15, 0.0); // грудь
		} else if (f < 0.6f) {
			double side = (player.tickCount % 2 == 0) ? 0.45 : -0.45;
			return base.add(side, 1.05, 0.0); // руки
		} else if (f < 0.85f) {
			double side = (player.tickCount % 2 == 0) ? 0.15 : -0.15;
			return base.add(side, 0.45, 0.0); // ноги
		}
		return base.add(0.0, 1.65, 0.0); // шлем — последним
	}

	/** Ремонтные частицы «латают» броню после урона. */
	private static void spawnRepairParticles(Minecraft mc, AbstractClientPlayer player) {
		if (mc.level == null) {
			return;
		}
		for (int i = 0; i < 3; i++) {
			double x = player.getX() + (mc.level.random.nextDouble() - 0.5) * 0.7;
			double y = player.getY() + 0.2 + mc.level.random.nextDouble() * 1.5;
			double z = player.getZ() + (mc.level.random.nextDouble() - 0.5) * 0.7;
			mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0, 0.06, 0.0);
			if (i == 0) {
				mc.level.addParticle(ParticleTypes.SCRAPE, x, y, z, 0.0, 0.03, 0.0);
			}
		}
	}

	/** Активная анимация сборки/разборки. */
	public static final class Anim {
		public final boolean disassemble;
		public final ResourceLocation texture;
		public final long startMillis;

		Anim(boolean disassemble, ResourceLocation texture, long startMillis) {
			this.disassemble = disassemble;
			this.texture = texture;
			this.startMillis = startMillis;
		}
	}

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		MAPS.lastSuit.clear();
		MAPS.anims.clear();
		MAPS.repairs.clear();
	}
}
