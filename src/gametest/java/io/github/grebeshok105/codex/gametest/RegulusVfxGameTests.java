package io.github.grebeshok105.codex.gametest;

import com.mojang.authlib.GameProfile;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.net.VfxChannelS2CPayload;
import io.github.grebeshok105.codex.core.net.VfxEventS2CPayload;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import io.github.grebeshok105.codex.hero.regulus.RegulusAbilities;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.ability.LionHeartAbility;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusHearts;
import io.github.grebeshok105.codex.hero.regulus.vfx.RegulusVfxIds;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Task 9 (rework): the Regulus Visual Core seam — {@code regulus/anim/*} clip
 * events ride {@code VfxEventS2CPayload} (tracking AND self), and the continuous
 * presentations ride {@code VfxChannelS2CPayload} streams
 * ({@code regulus/greed_magnet}, {@code regulus/greed_stasis},
 * {@code regulus/lion_heart_dome}, {@code regulus/heart_pulse}). Asserts the
 * wire contract from the caster's own connection: START, heartbeat UPDATEs
 * inside the client-side 10-tick silence timeout, and STOP.
 */
public final class RegulusVfxGameTests implements FabricGameTest {

	/**
	 * Lion-heart activation: one {@code regulus/anim/lion_heart_activation}
	 * event reaches the caster's own connection (the trackingAndSelf path),
	 * the {@code regulus/lion_heart_dome} channel opens, heartbeats inside the
	 * 10-tick timeout, and deactivate closes it.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void animPayloadBroadcastToTrackingAndSelf(GameTestHelper helper) {
		Wire regulus = joinAudible(helper, "vfx-regulus");
		TestHeroes.transform(regulus.player(), RegulusHero.ID);
		// Midair isolation: an active lion heart freezes projectiles, pushes
		// entities on deactivate, and claims hearts — any neighbour pad's
		// entities in range would get owned/pushed by a foreign regulus.
		regulus.player().teleportTo(regulus.player().getX() + 512.0,
				regulus.player().getY() + 160.0, regulus.player().getZ() + 512.0);
		regulus.player().setNoGravity(true);

		drain(regulus.channel());
		helper.runAfterDelay(5, () -> {
			// Ability path, not the controller: tryActivate sends the anim event,
			// the cast fires LionHeartController.activate (dome START) at tick 14.
			AbilityRouter.activate(regulus.player(), LionHeartAbility.ID);
			List<VfxEventS2CPayload> anim = new ArrayList<>();
			List<VfxChannelS2CPayload> dome = new ArrayList<>();
			helper.runAfterDelay(40, () -> {
				AbilityRouter.deactivate(regulus.player(), LionHeartAbility.ID);
				await(helper, () -> {
					for (Object o : drain(regulus.channel())) {
						if (o instanceof ClientboundCustomPayloadPacket custom) {
							if (custom.payload() instanceof VfxEventS2CPayload event
									&& event.effect().equals(RegulusVfxIds.ANIM_LION_HEART_ACTIVATION)) {
								anim.add(event);
							} else if (custom.payload() instanceof VfxChannelS2CPayload ch
									&& ch.channel().equals(RegulusVfxIds.CHANNEL_LION_HEART_DOME)) {
								dome.add(ch);
							}
						}
					}
					return !anim.isEmpty() && !dome.isEmpty()
							&& dome.get(dome.size() - 1).state() == VfxChannelS2CPayload.STOP;
				}, 20, () -> {
					helper.assertTrue(anim.size() == 1,
							"exactly one lion_heart_activation anim event, got " + anim.size());
					helper.assertTrue(anim.get(0).sourceEntityId() == regulus.player().getId(),
							"anim event carries the caster entity id");
					helper.assertTrue(!dome.isEmpty()
									&& dome.get(0).state() == VfxChannelS2CPayload.START,
							"first dome packet is START, got states " + states(dome));
					long updates = dome.stream()
							.filter(p -> p.state() == VfxChannelS2CPayload.UPDATE).count();
					helper.assertTrue(updates >= 1,
							"dome channel keepalive inside the 10t timeout, got states " + states(dome));
					helper.assertTrue(dome.get(dome.size() - 1).state() == VfxChannelS2CPayload.STOP,
							"deactivate closes the dome channel, got states " + states(dome));
					TestPlayers.leave(regulus.player());
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Heart-pulse channel: opens the first tick the owner's bearer set goes
	 * non-empty and STOPs on hero clear ({@code dropAll}).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void heartPulseChannelTracksHeartSet(GameTestHelper helper) {
		Wire regulus = joinAudible(helper, "pulse-regulus");
		TestHeroes.transform(regulus.player(), RegulusHero.ID);
		// Midair isolation: hearts are claimed by any regulus inside 20 blocks;
		// sibling gametests share the world (see RegulusHeartsGameTests#isolate).
		regulus.player().teleportTo(regulus.player().getX() + 640.0,
				regulus.player().getY() + 160.0, regulus.player().getZ());
		regulus.player().setNoGravity(true);

		Pig pig = spawnEntity(helper, EntityType.PIG,
				regulus.player().getX() + 2.0, regulus.player().getY(), regulus.player().getZ());

		List<VfxChannelS2CPayload> pulse = new ArrayList<>();
		drain(regulus.channel());
		await(helper, () -> {
			collectChannel(drain(regulus.channel()), RegulusVfxIds.CHANNEL_HEART_PULSE,
					regulus.player().getId(), pulse);
			return RegulusHearts.count(regulus.player()) >= 1
					&& pulse.stream().anyMatch(p -> p.state() == VfxChannelS2CPayload.START);
		}, 100, () -> {
			helper.assertTrue(RegulusHearts.count(regulus.player()) >= 1,
					"precondition: the pig carries a heart");
			helper.assertTrue(pulse.get(0).state() == VfxChannelS2CPayload.START,
					"pulse channel opens on the empty->non-empty transition, got " + states(pulse));
			// Transform carries a 20-tick cooldown — untransform must wait it out.
			helper.runAfterDelay(25, () -> {
				helper.assertTrue(HeroTransformService.untransform(regulus.player()),
						"untransform succeeds once the transform cooldown has passed");
				await(helper, () -> {
					collectChannel(drain(regulus.channel()), RegulusVfxIds.CHANNEL_HEART_PULSE,
							regulus.player().getId(), pulse);
					return pulse.stream().anyMatch(p -> p.state() == VfxChannelS2CPayload.STOP);
				}, 20, () -> {
					helper.assertTrue(pulse.get(pulse.size() - 1).state() == VfxChannelS2CPayload.STOP,
							"hero clear STOPs the pulse channel, got " + states(pulse));
					releaseIsolation(helper, regulus.player());
					TestPlayers.leave(regulus.player());
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Greed-magnet channel: START when the magnet engages on the aimed victim,
	 * heartbeat UPDATEs while held, STOP on release.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void greedMagnetChannelFollowsMagnetLifetime(GameTestHelper helper) {
		Wire regulus = joinAudible(helper, "magnet-regulus");
		TestHeroes.transform(regulus.player(), RegulusHero.ID);
		regulus.player().teleportTo(regulus.player().getX() + 768.0,
				regulus.player().getY() + 160.0, regulus.player().getZ());
		regulus.player().setNoGravity(true);

		Zombie zombie = spawnEntity(helper, EntityType.ZOMBIE,
				regulus.player().getX() + 4.0, regulus.player().getY(), regulus.player().getZ());

		List<VfxChannelS2CPayload> magnet = new ArrayList<>();
		awaitGreedSees(helper, regulus.player(), zombie, 40, () -> {
			regulus.player().lookAt(EntityAnchorArgument.Anchor.EYES,
					zombie.getBoundingBox().getCenter());
			drain(regulus.channel());
			AbilityRouter.activate(regulus.player(), RegulusAbilities.MANIA_OF_GREED);
			helper.runAfterDelay(8, () -> {
				AbilityRouter.deactivate(regulus.player(), RegulusAbilities.MANIA_OF_GREED);
				await(helper, () -> {
					collectChannel(drain(regulus.channel()), RegulusVfxIds.CHANNEL_GREED_MAGNET,
							regulus.player().getId(), magnet);
					return !magnet.isEmpty()
							&& magnet.get(magnet.size() - 1).state() == VfxChannelS2CPayload.STOP;
				}, 20, () -> {
					helper.assertTrue(!magnet.isEmpty()
									&& magnet.get(0).state() == VfxChannelS2CPayload.START,
							"magnet channel opens on cast, got " + states(magnet));
					long updates = magnet.stream()
							.filter(p -> p.state() == VfxChannelS2CPayload.UPDATE).count();
					helper.assertTrue(updates >= 3,
							"magnet channel heartbeats every "
									+ "CHANNEL_UPDATE_INTERVAL_TICKS, got states " + states(magnet));
					helper.assertTrue(magnet.get(magnet.size() - 1).state() == VfxChannelS2CPayload.STOP,
							"release STOPs the magnet channel, got " + states(magnet));
					releaseIsolation(helper, regulus.player());
					TestPlayers.leave(regulus.player());
					helper.succeed();
				});
			});
		});
	}

	// ---- helpers ------------------------------------------------------------

	/** A joined player whose raw outbound packets stay readable for assertions. */
	private record Wire(ServerPlayer player, EmbeddedChannel channel) {
	}

	private static Wire joinAudible(GameTestHelper helper, String name) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), name);
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				profile, cookie.clientInformation());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		EmbeddedChannel channel = new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		return new Wire(player, channel);
	}

	/** Retries {@code ready} once per tick until it holds; drains may accumulate packets. */
	private static void await(GameTestHelper helper, BooleanSupplier ready, int tries, Runnable done) {
		if (ready.getAsBoolean()) {
			done.run();
			return;
		}
		helper.assertTrue(tries > 0, "condition never became true");
		helper.runAfterDelay(1, () -> await(helper, ready, tries - 1, done));
	}

	private static List<Object> drain(EmbeddedChannel channel) {
		channel.runPendingTasks();
		List<Object> out = new ArrayList<>();
		for (Object o = channel.readOutbound(); o != null; o = channel.readOutbound()) {
			out.add(o);
		}
		return out;
	}

	private static void collectChannel(List<Object> packets,
			net.minecraft.resources.ResourceLocation channelId, int entityId,
			List<VfxChannelS2CPayload> out) {
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof VfxChannelS2CPayload channel
					&& channel.channel().equals(channelId)
					&& channel.entityId() == entityId) {
				out.add(channel);
			}
		}
	}

	private static String states(List<VfxChannelS2CPayload> packets) {
		StringBuilder sb = new StringBuilder("[");
		for (VfxChannelS2CPayload p : packets) {
			sb.append(p.state()).append(',');
		}
		return sb.append(']').toString();
	}

	private static <T extends net.minecraft.world.entity.Entity> T spawnEntity(
			GameTestHelper helper, EntityType<T> type, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		helper.getLevel().setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
		T entity = type.create(helper.getLevel());
		entity.moveTo(x, y, z, 0f, 0f);
		entity.setNoGravity(true);
		if (entity instanceof net.minecraft.world.entity.Mob mob) {
			mob.setNoAi(true);
		}
		helper.getLevel().addFreshEntity(entity);
		return entity;
	}

	/** Free the far chunk tickets parked by isolation. */
	private static void releaseIsolation(GameTestHelper helper, ServerPlayer player) {
		BlockPos pos = player.blockPosition();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.getLevel().setChunkForced((pos.getX() >> 4) + dx, (pos.getZ() >> 4) + dz, false);
			}
		}
	}

	/**
	 * Poll the spatial-section query shape {@code findTarget} runs: a fresh mob
	 * can be index-visible a tick or two before the section sees it.
	 */
	private static void awaitGreedSees(GameTestHelper helper, ServerPlayer player,
			net.minecraft.world.entity.LivingEntity victim, int tries, Runnable body) {
		Vec3 look = player.getViewVector(1.0f);
		AABB box = player.getBoundingBox().expandTowards(look.scale(100.0)).inflate(1.5);
		if (tries <= 0
				|| helper.getLevel().getEntities(player, box, e -> e == victim).contains(victim)) {
			body.run();
			return;
		}
		helper.runAfterDelay(1, () -> awaitGreedSees(helper, player, victim, tries - 1, body));
	}
}
