package io.github.grebeshok105.codex.client.hero.homelander.emf;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmfExpressionBuilderTest {

	private static EmfModelData model() {
		return new EmfModelData(
				Map.of(
						"root", new EmfModelData.Bone("root", null, new float[]{0, 12, 0}, List.of()),
						"body", new EmfModelData.Bone("body", "root", new float[]{0, 12, 0}, List.of()),
						"head", new EmfModelData.Bone("head", "body", new float[]{0, 24, 0}, List.of())),
				List.of("root", "body", "head"),
				Map.of(0, ResourceLocation.parse("superheroes:textures/entity/hero/homelander.png")),
				64, 64, Set.of());
	}

	private static EmfClip clip(String name, boolean loop, int frames,
			Map<String, Map<EmfClip.Channel, float[][]>> tracks) {
		return new EmfClip(ResourceLocation.parse("superheroes:" + name),
				frames, 60, loop, false, tracks);
	}

	@Test
	void loopClipEmitsKeyframeloopWeightedByVar() {
		EmfClip hover = clip("hover", true, 2, Map.of(
				"head", Map.of(EmfClip.Channel.ROTATION,
						new float[][]{{10f, 0f, 0f}, {20f, 0f, 0f}})));
		EmfExpressionBuilder.Built built = EmfExpressionBuilder.build(model(), List.of(hover));
		EmfExpressionBuilder.Line rx = built.lines().stream()
				.filter(l -> l.animKey().equals("head.rx")).findFirst().orElseThrow();
		// bb +x rotation negates to vanilla -x, degrees to radians.
		assertEquals("var.hover_w * keyframeloop(var.hover_t, "
						+ Float.toString((float) Math.toRadians(-10f)) + ", "
						+ Float.toString((float) Math.toRadians(-20f)) + ")",
				rx.expression());
	}

	@Test
	void positionLineIncludesRestOffsetAndConvertsAxes() {
		EmfClip hover = clip("hover", true, 1, Map.of(
				"head", Map.of(EmfClip.Channel.POSITION,
						new float[][]{{1f, 2f, 3f}})));
		EmfExpressionBuilder.Built built = EmfExpressionBuilder.build(model(), List.of(hover));
		EmfExpressionBuilder.Line ty = built.lines().stream()
				.filter(l -> l.animKey().equals("head.ty")).findFirst().orElseThrow();
		// head rest offset = bb(0,24,0) - bb(0,12,0) = vanilla (0,-12,0);
		// clip delta bb y=+2 -> vanilla -2.
		assertEquals("-12 + var.hover_w * keyframeloop(var.hover_t, -2)", ty.expression());
	}

	@Test
	void rootLinesCarryLeanVars() {
		EmfClip hover = clip("hover", true, 1, Map.of(
				"root", Map.of(EmfClip.Channel.ROTATION,
						new float[][]{{0f, 0f, 0f}})));
		EmfExpressionBuilder.Built built = EmfExpressionBuilder.build(model(), List.of(hover));
		EmfExpressionBuilder.Line rx = built.lines().stream()
				.filter(l -> l.animKey().equals("root.rx")).findFirst().orElseThrow();
		assertTrue(rx.expression().startsWith("var.lean_pitch + "),
				() -> "root.rx should add lean pitch, got " + rx.expression());
		EmfExpressionBuilder.Line rz = built.lines().stream()
				.filter(l -> l.animKey().equals("root.rz")).findFirst().orElseThrow();
		assertTrue(rz.expression().startsWith("var.lean_roll + "));
		EmfExpressionBuilder.Line ty = built.lines().stream()
				.filter(l -> l.animKey().equals("root.ty")).findFirst().orElseThrow();
		assertTrue(ty.expression().endsWith(" + var.lean_y"));
		assertTrue(built.varNames().contains("var.lean_pitch"));
	}

	@Test
	void propBoneScaleRestsAtZero() {
		EmfModelData propModel = new EmfModelData(
				Map.of(
						"root", new EmfModelData.Bone("root", null, new float[]{0, 12, 0}, List.of()),
						"milk_bottle", new EmfModelData.Bone("milk_bottle", "root",
								new float[]{4, 10, 0}, List.of())),
				List.of("root", "milk_bottle"),
				Map.of(), 64, 64, Set.of("milk_bottle"));
		EmfClip milk = clip("milk_drink", false, 1, Map.of(
				"milk_bottle", Map.of(EmfClip.Channel.SCALE,
						new float[][]{{1f, 1f, 1f}})));
		EmfExpressionBuilder.Built built = EmfExpressionBuilder.build(propModel, List.of(milk));
		EmfExpressionBuilder.Line sx = built.lines().stream()
				.filter(l -> l.animKey().equals("milk_bottle.sx")).findFirst().orElseThrow();
		assertEquals("0 + var.milk_drink_w * 1", sx.expression());
	}

	@Test
	void multipleClipsSumOnSharedChannel() {
		EmfClip a = clip("hover", true, 1, Map.of(
				"head", Map.of(EmfClip.Channel.ROTATION, new float[][]{{5f, 0f, 0f}})));
		EmfClip b = clip("boost", true, 1, Map.of(
				"head", Map.of(EmfClip.Channel.ROTATION, new float[][]{{85f, 0f, 0f}})));
		EmfExpressionBuilder.Built built = EmfExpressionBuilder.build(model(), List.of(a, b));
		EmfExpressionBuilder.Line rx = built.lines().stream()
				.filter(l -> l.animKey().equals("head.rx")).findFirst().orElseThrow();
		assertTrue(rx.expression().contains("var.hover_w * keyframeloop(var.hover_t"));
		assertTrue(rx.expression().contains(" + var.boost_w * keyframeloop(var.boost_t"));
	}
}
