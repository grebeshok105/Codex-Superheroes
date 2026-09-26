package io.github.grebeshok105.codex.architecture;

import com.tngtech.archunit.library.freeze.FreezingArchRule;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Plan 5 / M1: the mechanic services own the velocity-sync and FX-audience
 * idioms. Existing hero call sites are frozen debt that shrinks per wave.
 */
class MechanicServiceRulesTest {
	static final String ROOT = CodexClasses.ROOT;

	@Test
	void motionPacketsGoThroughMotion() {
		FreezingArchRule.freeze(noClasses().that().doNotHaveSimpleName("Motion")
				.should().callConstructor(ClientboundSetEntityMotionPacket.class, Entity.class)
			.orShould().callConstructor(ClientboundSetEntityMotionPacket.class, int.class, Vec3.class)
				.as("velocity changes go through mechanic.motion.Motion")).check(CodexClasses.main());
	}

	@Test
	void fxAudiencesGoThroughFxBroadcast() {
		FreezingArchRule.freeze(noClasses().that().resideOutsideOfPackage(ROOT + ".core.net..")
				.should().callMethodWhere(target(owner(simpleName("PlayerLookup")))
						.and(target(name("tracking").or(name("around")))))
				.as("FX audiences go through core.net.FxBroadcast")).check(CodexClasses.main());
	}
}
