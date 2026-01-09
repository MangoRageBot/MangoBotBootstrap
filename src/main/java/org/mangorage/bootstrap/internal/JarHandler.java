package org.mangorage.bootstrap.internal;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class JarHandler {

    interface UnsafeRunnable {
        void run() throws Exception;
    }

    enum ModuleNameOrigin {
        MODULE_INFO, // Takes Highest priority
        MANIFEST,
        GUESSED // Takes lowest
    }

    record Result(String name, ModuleNameOrigin origin, Path jar, AtomicReference<UnsafeRunnable> task) {
        public Result(String name, ModuleNameOrigin origin, Path jar) {
            this(name, origin, jar, new AtomicReference<>());
        }
    }

    static void safeHandle(final Path source, final Path target) {
        try {
            handle(source, target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void handle(final Path source, final Path target) throws IOException {

        if (Files.exists(target)) {
            deleteDirectory(target);
        }

        Files.createDirectories(target);

        Map<String, Result> seenModules = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source, "*.jar")) {
            for (Path jar : stream) {
                var module = resolveModuleName(jar);
                if (module == null) {
                    System.out.println("Skipping non-module JAR: " + jar);
                    continue;
                }

                if (!seenModules.containsKey(module.name())) {
                    Path dest = target.resolve(jar.getFileName());
                    module.task().set(() -> Files.copy(jar, dest, StandardCopyOption.REPLACE_EXISTING));
                    seenModules.put(module.name(), module);
                    System.out.println("Added module: " + module.name() + " from " + jar + " Search Origin: " + module.origin());
                } else {
                    if (seenModules.get(module.name()).origin() == ModuleNameOrigin.GUESSED && module.origin() != ModuleNameOrigin.GUESSED) {
                        var oldModule = seenModules.get(module.name());
                        Path dest = target.resolve(jar.getFileName());
                        module.task().set(() -> Files.copy(jar, dest, StandardCopyOption.REPLACE_EXISTING));
                        seenModules.put(module.name(), module);
                        System.out.println("Swapped module: " + module.name() + " jar to " + jar + " from" + oldModule.jar() + " Search Origin: " + module.origin());
                        continue;
                    }

                    System.out.println("Duplicate module ignored: " + module.name() + " from " + jar + " Search Origin: " + module.origin());
                }
            }
        }

        seenModules.remove("gson"); // HACK FIX

        seenModules.forEach((string, result) -> {
            final var runnable = result.task().get();
            if (runnable != null)
                try {
                    runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        });

        System.out.println("Finished deduplicating modules. Result at: " + target);
    }

    static boolean check(Path path) {
        return path.toString().contains("okio") && !path.toString().contains("jvm");
    }

    private static void deleteDirectory(Path dir) throws IOException {
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private static Result resolveModuleName(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {

            String moduleName = null;

            try {
                moduleName = ModuleFinder.of(jarPath)
                        .findAll()
                        .iterator()
                        .next()
                        .descriptor()
                        .name();
            } catch (Exception ignore) {}

            // 1. Proper JPMS module
            if (jarFile.getEntry("module-info.class") != null) {
                return new Result(
                        ModuleFinder.of(jarPath)
                                .findAll()
                                .iterator()
                                .next()
                                .descriptor()
                                .name(),
                        ModuleNameOrigin.MODULE_INFO,
                        jarPath
                );
            } else if (jarFile.isMultiRelease() && moduleName != null) {
                return new Result(moduleName, ModuleNameOrigin.GUESSED, jarPath);
            }

            // 2. Check MANIFEST.MF for Automatic-Module-Name
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                String autoName = manifest.getMainAttributes()
                        .getValue("Automatic-Module-Name");

                if (autoName != null && !autoName.isBlank()) {
                    return new Result(
                            autoName,
                            ModuleNameOrigin.MANIFEST,
                            jarPath
                    );
                }
            }

            // 3. Fallback: filename heuristic (aka desperation mode)
            String filename = jarPath.getFileName().toString();
            return new Result(
                    filename
                            .replaceAll("-[\\d\\.]+.*\\.jar$", "")
                            .replaceAll("\\.jar$", ""),
                    ModuleNameOrigin.GUESSED,
                    jarPath
            );

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read JAR: " + jarPath, e);
        }
    }

}
