package org.mangorage.bootstrap.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mangorage.bootstrap.internal.util.Dependencies;
import org.mangorage.bootstrap.internal.util.Result;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class DependencyHandler {
    private static final Gson GSON = new GsonBuilder().create();

    public static Map<String, List<Result>> scanPackages(Path packagesPath, Path librariesPath) throws IOException {

        final Map<String, List<Result>> results = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(packagesPath)) {
            for (Path entry : stream) {
                final Dependencies dependenciesList = GSON.fromJson(
                        readFileFromJar(entry, "installer-data/dependencies.json"),
                        Dependencies.class
                );

                dependenciesList.dependencies().forEach(dependency -> {
                    final var result = JarHandler.resolveModuleName(
                            librariesPath.resolve(dependency.output())
                    );
                    results.computeIfAbsent(result.name(), k -> new ArrayList<>()).add(result);
                });
            }
        }

        return results;
    }


    public static String readFileFromJar(Path jarPath, String entryPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(entryPath);

            if (entry == null) {
                throw new FileNotFoundException("Entry not found in jar: " + entryPath);
            }

            try (InputStream in = jar.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }
}
