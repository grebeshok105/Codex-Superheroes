package com.example.superheroes.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CodexClasses {
	static final String ROOT = "com.example.superheroes";
	private static JavaClasses main;
	private static JavaClasses mainAndClient;

	private CodexClasses() {
	}

	static synchronized JavaClasses main() {
		if (main == null) main = importDirs("codex.mainClasses");
		return main;
	}

	static synchronized JavaClasses mainAndClient() {
		if (mainAndClient == null) mainAndClient = importDirs("codex.mainClasses", "codex.clientClasses");
		return mainAndClient;
	}

	private static JavaClasses importDirs(String... properties) {
		List<Path> dirs = new ArrayList<>();
		for (String property : properties) {
			String value = System.getProperty(property);
			if (value == null || value.isBlank()) throw new IllegalStateException(property + " is not set");
			for (String entry : value.split(File.pathSeparator)) {
				Path dir = Path.of(entry);
				if (Files.isDirectory(dir)) dirs.add(dir);
			}
		}
		if (dirs.isEmpty()) throw new IllegalStateException("no class directories");
		return new ClassFileImporter().importPaths(dirs);
	}
}
