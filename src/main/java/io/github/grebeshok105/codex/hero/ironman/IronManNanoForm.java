package io.github.grebeshok105.codex.hero.ironman;

import com.mojang.serialization.Codec;
import io.github.grebeshok105.codex.core.attachment.AttachmentRegistrar;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Нано-формы оружия Mark 85: оружие «вырастает» из брони (Endgame-стиль).
 */
public enum IronManNanoForm {
	NONE(0, "off"),
	BLADE(1, "blade"),
	HAMMER(2, "hammer"),
	SHIELD(3, "shield");

	/**
	 * Player attachment holding the current nano-form index ({@code superheroes:nano_form},
	 * 0 initializer + persistent + copyOnDeath — byte-identical to the old {@code ModAttachments} row).
	 * Created eagerly at class-init — see {@link IronManAttachments#init()}.
	 */
	public static final AttachmentType<Integer> ATTACHMENT =
			AttachmentRegistrar.FABRIC.persistent("nano_form", Codec.INT, true, () -> 0);

	private final int index;
	private final String key;

	IronManNanoForm(int index, String key) {
		this.index = index;
		this.key = key;
	}

	public int index() {
		return index;
	}

	public String translationKey() {
		return "ability.superheroes.iron_man_nano_form." + key;
	}

	public static IronManNanoForm byIndex(int index) {
		return switch (index) {
			case 1 -> BLADE;
			case 2 -> HAMMER;
			case 3 -> SHIELD;
			default -> NONE;
		};
	}

	public IronManNanoForm next() {
		return byIndex((index + 1) % 4);
	}
}
