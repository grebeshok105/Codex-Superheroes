package com.example.superheroes.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bidirectional package pairs may only disappear. Edges that originate in composition roots
 * (entrypoints, module lists) are not counted: those classes depend on everything by design, and
 * nothingDependsOnCompositionRoots keeps them a sink.
 */
class PackageCycleRatchetTest {
	private static final Path BASELINE = Path.of("src/test/resources/architecture/package-cycles-baseline.txt");

	@Test
	void bidirectionalPackagePairsOnlyShrink() throws IOException {
		Set<String> edges = new HashSet<>();
		for (JavaClass origin : CodexClasses.mainAndClient()) {
			if (ArchitectureRulesTest.COMPOSITION_ROOT.test(origin)) {
				continue;
			}
			String from = relative(origin.getPackageName());
			if (from == null) {
				continue;
			}
			for (Dependency d : origin.getDirectDependenciesFromSelf()) {
				String to = relative(d.getTargetClass().getPackageName());
				if (to != null && !to.equals(from)) {
					edges.add(from + " -> " + to);
				}
			}
		}
		Set<String> pairs = new TreeSet<>();
		for (String edge : edges) {
			String[] p = edge.split(" -> ");
			if (edges.contains(p[1] + " -> " + p[0])) {
				pairs.add(p[0].compareTo(p[1]) < 0 ? p[0] + " <-> " + p[1] : p[1] + " <-> " + p[0]);
			}
		}
		Set<String> baseline = new TreeSet<>(Files.exists(BASELINE) ? Files.readAllLines(BASELINE) : java.util.List.of());
		baseline.removeIf(String::isBlank);
		if (Boolean.getBoolean("codex.writeCycleBaseline")) {
			Files.write(BASELINE, pairs);
			return;
		}
		assertEquals(String.join("\n", baseline), String.join("\n", pairs),
				"package 2-cycles changed: new pairs are forbidden; removed pairs must be deleted from " + BASELINE);
	}

	private static String relative(String pkg) {
		String root = CodexClasses.ROOT;
		if (pkg.equals(root)) return "(root)";
		return pkg.startsWith(root + ".") ? pkg.substring(root.length() + 1) : null;
	}
}
