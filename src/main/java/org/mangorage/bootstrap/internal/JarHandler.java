package org.mangorage.bootstrap.internal;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public final class JarHandler {

    public static void safeHandle(final Path source, final Path target) {
        try {
            handle(source, target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handle(final Path source, final Path target) throws IOException {

        if (Files.exists(target)) {
            deleteDirectory(target);
        }

        Files.createDirectories(target);

        Map<String, Path> seenModules = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source, "*.jar")) {
            for (Path jar : stream) {
                String moduleName = resolveModuleName(jar);
                if (moduleName == null) {
                    System.out.println("Skipping non-module JAR: " + jar);
                    continue;
                }

                if (!seenModules.containsKey(moduleName)) {
                    Path dest = target.resolve(jar.getFileName());
                    Files.copy(jar, dest, StandardCopyOption.REPLACE_EXISTING);
                    seenModules.put(moduleName, jar);
                    System.out.println("Added module: " + moduleName);
                } else {
                    System.out.println("Duplicate module ignored: " + moduleName + " from " + jar);
                }
            }
        }

        System.out.println("Finished deduplicating modules. Result at: " + target);
    }

    private static void deleteDirectory(Path dir) throws IOException {
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private static String resolveModuleName(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            ZipEntry entry = jarFile.getEntry("module-info.class");
            if (entry != null) {
                // This is a proper JPMS module JAR
                return ModuleFinder.of(jarPath).findAll().iterator().next().descriptor().name();
            } else {
                // Fall back to heuristic based on filename (best effort)
                String filename = jarPath.getFileName().toString();
                return filename.replaceAll("-[\\d\\.]+.*\\.jar$", "").replaceAll("\\.jar$", "");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
