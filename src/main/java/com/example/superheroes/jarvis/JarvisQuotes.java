package com.example.superheroes.jarvis;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class JarvisQuotes {
	private JarvisQuotes() {
	}

	private static final List<String> DETECT_NORMAL = List.of(
			"Сэр, обнаружена угроза. Рекомендую повышенную осторожность.",
			"Фиксирую боевую сигнатуру. Классификация в процессе.",
			"Сэр, на радаре контакт. Оцениваю уровень опасности.",
			"Новая цель обнаружена. Анализ...",
			"Внимание, сэр. Зафиксирована активность в зоне сканирования."
	);

	private static final List<String> DETECT_S_TIER = List.of(
			"Сэр... Это... Это запредельная угроза. Настоятельно рекомендую отступление!",
			"Критическое предупреждение! Сигнатура класса S. Все системы в боевой режим!",
			"Сэр, я фиксирую нечто... нечто что выходит за рамки наших протоколов.",
			"Боевая тревога! Уровень угрозы превышает все расчётные параметры!",
			"Сэр, показатели мощности цели... они запредельны. Будьте крайне осторожны."
	);

	private static final List<String> SUIT_SWITCH = List.of(
			"Загружаю пресет костюма... Калибровка завершена.",
			"Переконфигурация наносистемы... Готово.",
			"Костюм адаптирован. Системы перезапущены.",
			"Новая конфигурация загружена, сэр. Все модули в норме."
	);

	private static final List<String> LOW_ENERGY = List.of(
			"Сэр, запасы энергии критически низки. Переход в режим экономии.",
			"Энергия на исходе. Рекомендую прекратить боевые действия.",
			"Внимание: уровень заряда критический. Ресурсы почти исчерпаны."
	);

	private static final List<String> FLIGHT_ACTIVATE = List.of(
			"Системы полёта активированы. Стабилизаторы в норме.",
			"Двигатели запущены. Набираю высоту.",
			"Полёт разрешён. Все системы стабильны."
	);

	private static final List<String> LEGION_DEPLOY = List.of(
			"Войско развёрнуто, сэр. Протокол «Легион» активен.",
			"Дроны на позициях. Автономный боевой режим включён.",
			"Железный Легион в бою. Координирую действия."
	);

	public static String randomDetect(JarvisThreatClass threat) {
		if (threat.usesExcitedSound()) {
			return pick(DETECT_S_TIER);
		}
		return pick(DETECT_NORMAL);
	}

	public static String randomSuitSwitch() {
		return pick(SUIT_SWITCH);
	}

	public static String randomLowEnergy() {
		return pick(LOW_ENERGY);
	}

	public static String randomFlightActivate() {
		return pick(FLIGHT_ACTIVATE);
	}

	public static String randomLegionDeploy() {
		return pick(LEGION_DEPLOY);
	}

	private static String pick(List<String> list) {
		return list.get(ThreadLocalRandom.current().nextInt(list.size()));
	}
}
