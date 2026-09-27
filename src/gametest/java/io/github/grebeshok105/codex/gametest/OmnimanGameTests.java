package io.github.grebeshok105.codex.gametest;

import com.mojang.authlib.GameProfile;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.hero.omniman.ability.OmnimanThinkMarkAbility;
import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityCooldowns;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.ability.AbilityRouter;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.lifecycle.ControlLockKind;
import io.github.grebeshok105.codex.hero.omniman.net.ThinkMarkS2CPayload;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Characterization for the I3 module move, written against the pre-move layout:
 * Omni-Man's ability list, the Viltrumite Rush session lifetime, the Think Mark
 * grab lock + pose broadcast, and the Homelander↔Omni-Man reaction sound that
 * fires on {@code HeroLifecycle.onTransformed}.
 */
public final class OmnimanGameTests implements FabricGameTest {
	private static final ResourceLocation REACTION_SOUND = ModId.of("homelander.omniman_react");

	@GameTest(template = EMPTY_STRUCTURE)
	public void omnimanOwnsItsAbilitiesInSlotOrder(GameTestHelper helper) {
		Hero omniman = Heroes.get(ModId.of("omniman"));
		helper.assertTrue(omniman != null, "omniman registered");
		helper.assertTrue(omniman.getAbilities().equals(List.of(
				ModId.of("flight"), ModId.of("omniman_viltrumite_rush"),
				ModId.of("omniman_think_mark"), ModId.of("omniman_world_breaker"),
				ModId.of("viltrumite_recovery"))),
				"slot order " + omniman.getAbilities());
		for (ResourceLocation id : omniman.getAbilities()) {
			helper.assertTrue(AbilityRegistry.get(id) != null, id + " registered");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void viltrumiteRushExpiresAfterItsDuration(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("omniman"));
		Ability rush = AbilityRegistry.get(ModId.of("omniman_viltrumite_rush"));

		AbilityRouter.activate(player, rush.getId());
		helper.assertFalse(rush.canActivate(player), "rush session started");
		helper.assertTrue(AbilityCooldowns.isOnCooldown(player, rush.getId()), "cooldown armed");
		helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0.1, "rush impulse applied");

		helper.runAfterDelay(8, () -> {
			helper.assertTrue(rush.canActivate(player), "4-tick rush is over");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void thinkMarkGrabLocksTargetAndBroadcastsPose(GameTestHelper helper) {
		Wire omniman = joinAudible(helper, "i3-grab-omni");
		Wire bystander = joinAudible(helper, "i3-grab-watch");
		TestHeroes.transform(omniman.player(), ModId.of("omniman"));
		Zombie zombie = spawnAhead(helper, omniman.player());

		TestPlayers.awaitVisible(helper, zombie, () -> {
			drain(omniman.channel());
			drain(bystander.channel());
			AbilityRouter.activate(omniman.player(), ModId.of("omniman_think_mark"));

			helper.assertTrue(OmnimanThinkMarkAbility.isActive(omniman.player()), "grab session started");
			helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI)
							.contains(omniman.player().getUUID()),
					"grab holds the NO_AI lock on the victim");
			helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_PHYSICS)
							.contains(omniman.player().getUUID()),
					"grab holds the NO_PHYSICS lock on the victim");
			helper.assertTrue(hasThinkMark(drain(bystander.channel()), true),
					"pose-on broadcast reached a bystander");

			OmnimanThinkMarkAbility.clear(omniman.player());
			helper.assertFalse(OmnimanThinkMarkAbility.isActive(omniman.player()), "clear ends the session");
			helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).isEmpty()
							&& TestPlayers.lockOwners(zombie, ControlLockKind.NO_PHYSICS).isEmpty(),
					"clear releases all grab locks");
			helper.assertTrue(hasThinkMark(drain(bystander.channel()), false),
					"pose-off broadcast reached a bystander");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void thinkMarkReleasesTargetWhenOmnimanLeaves(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestHeroes.transform(player, ModId.of("omniman"));
		Zombie zombie = spawnAhead(helper, player);

		TestPlayers.awaitVisible(helper, zombie, () -> {
			AbilityRouter.activate(player, ModId.of("omniman_think_mark"));
			helper.assertTrue(OmnimanThinkMarkAbility.isActive(player), "grab session started");
			helper.assertFalse(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).isEmpty(),
					"victim is locked");

			TestPlayers.leave(player);
			helper.assertFalse(OmnimanThinkMarkAbility.isActive(player), "logout ends the session");
			helper.assertTrue(TestPlayers.lockOwners(zombie, ControlLockKind.NO_AI).isEmpty()
							&& TestPlayers.lockOwners(zombie, ControlLockKind.NO_PHYSICS).isEmpty(),
					"logout releases all grab locks");
			helper.succeed();
		});
	}

	/**
	 * The Homelander↔Omni-Man bark is a broadcast to every player in the level when one
	 * side transforms while the other is present. Unrelated transformations stay silent.
	 * All four players transform exactly once — the 20-tick transform cooldown never bites.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void reactionSoundBroadcastsOnlyForTheHomelanderOmnimanPairing(GameTestHelper helper) {
		Wire homelander = joinAudible(helper, "i3-react-hl");
		Wire outsider = joinAudible(helper, "i3-react-sc");
		Wire omniman = joinAudible(helper, "i3-react-om");
		Wire secondHomelander = joinAudible(helper, "i3-react-h2");

		TestHeroes.transform(homelander.player(), ModId.of("homelander"));
		drainAll(homelander, outsider, omniman, secondHomelander);

		// Unrelated hero appearing next to a Homelander: no reaction.
		TestHeroes.transform(outsider.player(), ModId.of("scorpion"));
		helper.assertFalse(hasReactionSound(drain(homelander.channel())), "no bark for scorpion");
		helper.assertFalse(hasReactionSound(drain(outsider.channel())), "no bark for scorpion");
		drain(omniman.channel());
		drain(secondHomelander.channel());

		// Omni-Man appearing next to a Homelander: everyone hears it.
		TestHeroes.transform(omniman.player(), ModId.of("omniman"));
		helper.assertTrue(hasReactionSound(drain(homelander.channel())), "bark reached the homelander");
		helper.assertTrue(hasReactionSound(drain(omniman.channel())), "bark reached the omniman");
		helper.assertTrue(hasReactionSound(drain(secondHomelander.channel())), "bark reached a bystander");
		drainAll(homelander, outsider, omniman, secondHomelander);

		// Mirror image: a Homelander appearing while an Omni-Man is around.
		TestHeroes.transform(secondHomelander.player(), ModId.of("homelander"));
		helper.assertTrue(hasReactionSound(drain(omniman.channel())), "bark reached the omniman");
		helper.assertTrue(hasReactionSound(drain(secondHomelander.channel())),
				"bark reached the new homelander");
		helper.succeed();
	}

	// ─────────────────────────── packet capture ───────────────────────────

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
		player.setGameMode(GameType.SURVIVAL);
		return new Wire(player, channel);
	}

	private static List<Object> drain(EmbeddedChannel channel) {
		List<Object> out = new ArrayList<>();
		for (Object o = channel.readOutbound(); o != null; o = channel.readOutbound()) {
			out.add(o);
		}
		return out;
	}

	private static void drainAll(Wire... wires) {
		for (Wire wire : wires) {
			drain(wire.channel());
		}
	}

	private static boolean hasReactionSound(List<Object> packets) {
		for (Object o : packets) {
			if (o instanceof ClientboundSoundPacket sound
					&& REACTION_SOUND.equals(sound.getSound().value().getLocation())) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasThinkMark(List<Object> packets, boolean active) {
		for (Object o : packets) {
			if (o instanceof ClientboundCustomPayloadPacket custom
					&& custom.payload() instanceof ThinkMarkS2CPayload pose
					&& pose.active() == active) {
				return true;
			}
		}
		return false;
	}

	private static Zombie spawnAhead(GameTestHelper helper, ServerPlayer player) {
		Vec3 ahead = player.position().add(player.getViewVector(1f).normalize().scale(2.0));
		Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
		zombie.moveTo(ahead.x, player.getY(), ahead.z, 0f, 0f);
		helper.getLevel().addFreshEntity(zombie);
		return zombie;
	}
}
