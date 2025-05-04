package org.mangorage.bootstrap.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Config {
    public static Config readFromInputStream(InputStream stream) {
        Map<String, String> config = new LinkedHashMap<>();

        if (stream == null) throw new IllegalArgumentException("Input stream is null, brainiac");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int sepIndex = line.indexOf('=');
                if (sepIndex <= 0 || sepIndex == line.length() - 1) continue; // malformed line

                String key = line.substring(0, sepIndex).trim();
                String value = line.substring(sepIndex + 1).trim();
                config.put(key, value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't read config from stream, congrats", e);
        }

        return new Config(config);
    }

    private final Map<String, String> cfg;

    Config(final Map<String, String> cfg) {
        this.cfg = cfg;
    }


    /**
     * Sort thru the jars and delete files we dont want...
     * @throws IOException
     */
    public void handleJars() throws IOException {
        if (cfg.containsKey("Sorted-Path")) {
            String[] folders = cfg.get("Sorted-Path").split(":");

            for (final var folder : folders) {
                final Path folderPath = Path.of(folder);
                JarHandler.handle(folderPath, Path.of(getSortedPath(folder)));
            }
        }

        if (cfg.containsKey("Delete-Files")) {
            String[] filesToDelete = cfg.get("Delete-Files").split(":");
            for (final var path : filesToDelete) {
                Files.deleteIfExists(Path.of(path));
            }
        }
    }

    /**
     * Constructs a chain of URLClassLoaders based on a colon-separated path string.
     * Each folder in the path string becomes a URLClassLoader, with the previous
     * classloader in the chain as its parent. The last classloader in the chain
     * is returned.
     *
     * Assumes the paths are relative to a base directory.
     *
     * @return The last URLClassLoader in the constructed chain, or the system
     * classloader if the Classloader-Path is empty or invalid.
     */
    public ClassLoader constructClassloaders() {
        final var baseDir = Path.of("");
        String classloaderPath = cfg.get("Classloader-Path");
        if (classloaderPath == null)
            throw new IllegalArgumentException("Need to define Classloader-Path");

        if (classloaderPath.trim().isEmpty()) {
            throw new IllegalStateException("Classloader-Path is empty. Define it...");
        }

        String[] folderNames = classloaderPath.split(":");
        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader().getParent();
        URLClassLoader currentClassLoader = null;

        for (String folderName : folderNames) {
            String trimmedFolderName = folderName.trim();
            if (trimmedFolderName.isEmpty()) {
                continue; // Skip empty entries from split
            }

            // Resolve the full path relative to the base directory
            Path folderPath = baseDir.resolve(trimmedFolderName);
            File folderFile = folderPath.toFile();

            // Ensure the path exists and is a directory
            if (!folderFile.exists() || !folderFile.isDirectory()) {
                System.err.println("Warning: Classloader path component '" + trimmedFolderName + "' is not a valid directory: " + folderPath.toAbsolutePath());
                // Depending on requirements, you might want to throw an exception here
                // or skip this path component. Skipping for now.
                continue;
            }

            // Create a new URLClassLoader with the current folder URL and the parent
            URL[] urls = Util.fetchJars(new File[]{folderFile});
            currentClassLoader = new URLClassLoader(urls, parentClassLoader);

            // The newly created classloader becomes the parent for the next iteration
            parentClassLoader = currentClassLoader;

        }

        // Return the last classloader created, or the system classloader if none were created
        return (currentClassLoader != null) ? currentClassLoader : Thread.currentThread().getContextClassLoader().getParent();
    }

    public Configuration constructModuleConfiguration() {
        if (cfg.containsKey("Module-Path")) {
            String[] modulePaths = cfg.get("Module-Path").split(":");

            Path[] paths = Arrays.stream(modulePaths)
                    .map(Path::of)
                    .toArray(Path[]::new);

            Path[] rootPaths = Arrays.stream(cfg.getOrDefault("Root-Module-Path", "").split(":"))
                    .map(Path::of)
                    .toArray(Path[]::new);

            ModuleFinder moduleFinder = ModuleFinder.of(paths);

            return ModuleLayer.boot()
                    .configuration()
                    .resolveAndBind(
                            moduleFinder,
                            ModuleFinder.of(),
                            rootPaths.length != 0 ? Util.getModuleNames(rootPaths[0]) : Set.of()
                    );
        } else {
            throw new IllegalStateException("Module-Path needs to be defined!");
        }
    }

    public String getMainClass() {
        return cfg.get("Main-Class");
    }

    public static String getSortedPath(String originalPath) {
        // Handle null or empty input
        if (originalPath == null || originalPath.isEmpty()) {
            return originalPath;
        }

        // Find the index of the last path separator
        int lastSeparatorIndex = originalPath.lastIndexOf('/');
        if (lastSeparatorIndex == -1) { // Check for backslash as well
            lastSeparatorIndex = originalPath.lastIndexOf('\\');
        }

        // If no separator is found, just prepend "sorted-" to the original path
        if (lastSeparatorIndex == -1) {
            return "sorted-" + originalPath;
        } else {
            // Get the part before the last separator (the directory)
            String directory = originalPath.substring(0, lastSeparatorIndex + 1);
            // Get the part after the last separator (the file/folder name)
            String name = originalPath.substring(lastSeparatorIndex + 1);

            // Construct the new path
            return directory + "sorted-" + name;
        }
    }
}
