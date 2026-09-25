package com.example.superheroes.gametest;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.ability.AbilityRouter;
import com.example.superheroes.effect.MirrorDimensionController;
import com.example.superheroes.hero.PandoraHero;
import com.example.superheroes.network.MirrorDimensionStatusC2SPayload;
import com.example.superheroes.transform.HeroTransformService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Audit B13: House of Vanity containment is server-authoritative. A victim whose
 * client never confirms the cosmetic warp packet — or has no mod at all — is
 * absorbed, debuffed and yanked back exactly like a confirmed one. Status answers
 * are feedback for Pandora, not a release condition.
 */
public final class HouseOfVanityGameTests implements FabricGameTest {

	private static Vec3 abs(GameTestHelper helper, double x, double y, double z) {
		return helper.absoluteVec(new Vec3(x, y, z));
	}

	/**
	 * Transforms a fresh mock player into Pandora standing at the House anchor.
	 * The caller places victims and only then activates the ability, so the
	 * initial absorb (not a keepalive) picks them up deterministically.
	 */
	private static ServerPlayer pandora(GameTestHelper helper) {
		ServerPlayer pandora = TestPlayers.join(helper);
		Vec3 p = abs(helper, 4, 2, 4);
		pandora.teleportTo(p.x, p.y, p.z);
		helper.assertTrue(HeroTransformService.transform(pandora, PandoraHero.ID), "pandora transform");
		return pandora;
	}

	private static ServerPlayer victimInside(GameTestHelper helper) {
		ServerPlayer victim = TestPlayers.join(helper);
		Vec3 inside = abs(helper, 6, 2, 6);
		victim.teleportTo(inside.x, inside.y, inside.z);
		return victim;
	}

	private static void walkOut(GameTestHelper helper, ServerPlayer victim) {
		Vec3 outside = abs(helper, 60, 2, 6);
		victim.teleportTo(outside.x, outside.y, outside.z);
	}

	private static void assertInsideZone(GameTestHelper helper, ServerPlayer victim, Vec3 center) {
		double dist = Math.sqrt(victim.distanceToSqr(center.x, victim.getY(), center.z));
		helper.assertTrue(dist < 50.0,
				"victim must be yanked back inside the 50-block zone (now " + dist + " out)");
	}

	/** A client that sends no confirmation at all is still contained (B13). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void houseTrapsVictimWithoutClientConfirmation(GameTestHelper helper) {
		ServerPlayer pandora = pandora(helper);
		ServerPlayer victim = victimInside(helper);
		Vec3 center = pandora.position();
		AbilityRouter.activate(pandora, AbilityIds.MIRROR_DIMENSION);

		helper.runAfterDelay(2, () -> {
			// Mock players cannot receive the S2C (no negotiated channel) and never
			// send a status — yet the server must already treat them as trapped.
			helper.assertTrue(MirrorDimensionController.isTrapped(victim),
					"victim absorbed without any client confirmation");
			walkOut(helper, victim);
		});
		helper.runAfterDelay(6, () -> {
			assertInsideZone(helper, victim, center);
			helper.assertTrue(MirrorDimensionController.isTrapped(victim),
					"silent victim is still a member of the zone");
			TestPlayers.leave(victim);
			TestPlayers.leave(pandora);
			helper.succeed();
		});
	}

	/** NO_IRIS (and by extension NO_PACK / IRIS_API_FAIL) is cosmetic feedback, not parole. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void noIrisStatusKeepsContainment(GameTestHelper helper) {
		ServerPlayer pandora = pandora(helper);
		ServerPlayer victim = victimInside(helper);
		Vec3 center = pandora.position();
		AbilityRouter.activate(pandora, AbilityIds.MIRROR_DIMENSION);

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(MirrorDimensionController.isTrapped(victim), "absorbed");
			MirrorDimensionController.handleStatus(victim, MirrorDimensionStatusC2SPayload.NO_IRIS);
		});
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(MirrorDimensionController.isTrapped(victim),
					"NO_IRIS victim stays in the House — status is only caster feedback (B13)");
			walkOut(helper, victim);
		});
		helper.runAfterDelay(9, () -> {
			assertInsideZone(helper, victim, center);
			TestPlayers.leave(victim);
			TestPlayers.leave(pandora);
			helper.succeed();
		});
	}

	/** The keepalive debuffs reach unconfirmed victims too. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void houseDebuffsVictimWithoutConfirmation(GameTestHelper helper) {
		ServerPlayer pandora = pandora(helper);
		ServerPlayer victim = victimInside(helper);
		AbilityRouter.activate(pandora, AbilityIds.MIRROR_DIMENSION);

		helper.runAfterDelay(25, () -> {
			helper.assertTrue(MirrorDimensionController.isTrapped(victim), "absorbed");
			helper.assertTrue(victim.hasEffect(MobEffects.CONFUSION)
							&& victim.hasEffect(MobEffects.DARKNESS)
							&& victim.hasEffect(MobEffects.WEAKNESS)
							&& victim.hasEffect(MobEffects.DIG_SLOWDOWN),
					"keepalive applies the maddening debuffs without a client ACK");
			TestPlayers.leave(victim);
			TestPlayers.leave(pandora);
			helper.succeed();
		});
	}

	/** The caster's leave closes the House and frees every victim (lifecycle hook). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void casterLeaveClosesHouseAndFreesVictims(GameTestHelper helper) {
		ServerPlayer pandora = pandora(helper);
		ServerPlayer victim = victimInside(helper);
		AbilityRouter.activate(pandora, AbilityIds.MIRROR_DIMENSION);

		helper.runAfterDelay(3, () -> {
			helper.assertTrue(MirrorDimensionController.isTrapped(victim), "absorbed");
			// Mock players report isCreative()==true, so the mayfly grant is skipped by
			// design here; the effects prove VanityAuthority.applyToCaster ran.
			helper.assertTrue(pandora.hasEffect(MobEffects.DAMAGE_RESISTANCE)
							&& pandora.hasEffect(MobEffects.FIRE_RESISTANCE),
					"Vanity Authority protects the caster while the House is open");
			TestPlayers.leave(pandora);
		});
		helper.runAfterDelay(6, () -> {
			helper.assertTrue(!MirrorDimensionController.isTrapped(victim),
					"victim freed the moment the caster left");
			helper.assertTrue(!pandora.hasEffect(MobEffects.DAMAGE_RESISTANCE),
					"caster cleared the moment the House is gone");
			TestPlayers.leave(victim);
			helper.succeed();
		});
	}
}
