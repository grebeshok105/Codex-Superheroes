package io.github.grebeshok105.codex.client.core.vfx;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@link VfxParams} JSON tuning: numbers, `#RRGGBB`/`#AARRGGBB` colors, forgiving parse. */
class VfxParamsTest {
	@Test
	void parsesNumbersAndColors() {
		JsonObject json = JsonParser.parseString(
				"{\"intensity\":1.5,\"core\":\"#FFE03020\",\"tint\":\"#30FFE0\"}").getAsJsonObject();

		VfxParams params = VfxParams.parse(json);

		assertEquals(1.5f, params.number("intensity", 0f));
		assertEquals(0xFFE03020, params.color("core", 0));
		assertEquals(0x30FFE0, params.color("tint", 0));
	}

	@Test
	void missingKeyReturnsFallback() {
		VfxParams params = VfxParams.parse(JsonParser.parseString("{}").getAsJsonObject());

		assertEquals(7f, params.number("missing", 7f));
		assertEquals(3, params.color("missing", 3));
	}

	@Test
	void malformedValueIgnored() {
		JsonObject json = JsonParser.parseString(
				"{\"num\":\"not a number\",\"col\":\"#GGGGGG\",\"shape\":{},\"other\":2.5}")
				.getAsJsonObject();

		VfxParams params = VfxParams.parse(json);

		assertEquals(9f, params.number("num", 9f));
		assertEquals(4, params.color("col", 4));
		assertEquals(4, params.color("shape", 4));
		assertEquals(2.5f, params.number("other", 0f));
	}

	@Test
	void withOverridesLayersOverlayValues() {
		VfxParams base = VfxParams.parse(JsonParser.parseString(
				"{\"boostPitch\":80,\"rollMax\":25,\"tint\":\"#102030\"}").getAsJsonObject());
		VfxParams overlay = VfxParams.parse(JsonParser.parseString(
				"{\"emfBoostRootPitch\":86,\"tint\":\"#A0B0C0\"}").getAsJsonObject());

		VfxParams merged = base.withOverrides(overlay);

		assertEquals(86f, merged.number("emfBoostRootPitch", 0f), "overlay adds new keys");
		assertEquals(80f, merged.number("boostPitch", 0f), "base keys survive");
		assertEquals(25f, merged.number("rollMax", 0f), "untouched base keys survive");
		assertEquals(0xA0B0C0, merged.color("tint", 0), "overlay wins on shared keys");
		// the inputs are records — unchanged by the merge
		assertEquals(0x102030, base.color("tint", 0));
	}

	@Test
	void withOverridesOnEmptyOverlayReturnsBase() {
		VfxParams base = VfxParams.parse(JsonParser.parseString("{\"a\":1}").getAsJsonObject());

		assertEquals(1f, base.withOverrides(VfxParams.EMPTY).number("a", 0f));
		assertEquals(1f, base.withOverrides(null).number("a", 0f), "null overlay is a no-op");
	}
}
