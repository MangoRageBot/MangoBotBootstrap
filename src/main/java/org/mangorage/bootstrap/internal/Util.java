package org.mangorage.bootstrap.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public final class Util {
    public static URL[] fetchJars(File[] directories) {
        // Add your extra folder here, you glutton for suffering

        List<URL> urls = new ArrayList<>();

        for (File dir : directories) {
            if (!dir.exists() || !dir.isDirectory()) continue;

            File[] jarFiles = dir.listFiles((d, name) -> name.endsWith(".jar"));
            if (jarFiles == null) continue;

            for (File jar : jarFiles) {
                try {
                    urls.add(jar.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Malformed URL while processing: " + jar.getAbsolutePath(), e);
                }
            }
        }

        return urls.toArray(URL[]::new);
    }

    public static Set<String> getModuleNames(Path folder) {
        final Set<String> moduleNames = new HashSet<>();

        if (folder == null || !folder.toFile().isDirectory()) {
            throw new IllegalArgumentException("That's not a valid folder, genius: " + folder);
        }

        final var jarFiles = folder.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) return moduleNames;

        for (final var jarFile : jarFiles) {
            final var name = getModuleName(jarFile);
            if (name != null) moduleNames.add(name);
        }

        return moduleNames;
    }

    public static String getModuleName(File jarFile) {
        if (jarFile == null || !jarFile.isFile() || !jarFile.getName().endsWith(".jar")) {
            throw new IllegalArgumentException("Not a valid jar file, genius: " + jarFile);
        }

        try {
            ModuleFinder finder = ModuleFinder.of(jarFile.toPath());
            Set<ModuleReference> modules = finder.findAll();

            for (ModuleReference moduleRef : modules) {
                var descriptor = moduleRef.descriptor();
                return descriptor.name(); // Return the first (and only) module name
            }
        } catch (Exception e) {
            System.err.println("Couldn't process " + jarFile.getName() + ": " + e.getMessage());
        }

        return null; // Jar was either not modular or you're just unlucky
    }

    public static void callMain(String className, String[] args, Module module) {
        try {
            Class<?> clazz = Class.forName(className, false, module.getClassLoader());
            Method mainMethod = clazz.getMethod("main", String[].class);

            // Make sure it's static and public
            if (!java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers())) {
                throw new IllegalStateException("Main method is not static, are you high?");
            }

            // Invoke the main method with a godawful cast
            mainMethod.invoke(null, (Object) args);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't reflectively call main because something exploded.", e);
        }
    }

    /**
     * Finds the first occurrence of a "boot.cfg" file within any JAR file
     * located in the specified root directory or its subdirectories.
     *
     * @param root The root directory to start the search from. Must be a valid directory.
     * @return An Optional containing an InputStream of the first found "boot.cfg",
     * or an empty Optional if not found.
     * @throws IllegalArgumentException if the root path is null or not a directory.
     * @throws UncheckedIOException if an I/O error occurs while walking the file tree.
     */
    public static Optional<Config> findBootConfig(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("That's not a valid directory, Einstein: " + root);
        }

        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    // Filter for paths ending with .jar
                    .filter(p -> p.toString().endsWith(".jar"))
                    // Convert Path to File
                    .map(Path::toFile)
                    // Filter to ensure it's actually a file (not a directory ending in .jar)
                    .filter(File::isFile)
                    // Attempt to get the boot config InputStream from the JAR
                    .map(jar -> getBootConfigFromJar(jar, "boot-data/boot.cfg"))
                    // Filter out null results (JARs that don't contain the resource)
                    .filter(Objects::nonNull)
                    // Take the first non-null InputStream found
                    .findFirst();
        } catch (IOException e) {
            // Wrap IOException in UncheckedIOException for stream processing
            throw new UncheckedIOException("Couldn't walk the file tree without falling on my face", e);
        }
    }

    /**
     * Attempts to get an InputStream for a specific resource name from within a JAR file.
     *
     * @param jarFile The JAR file to search within.
     * @param resourceName The name of the resource to find (e.g., "boot-data/boot.cfg").
     * @return An InputStream for the resource if found, otherwise null.
     */
    private static Config getBootConfigFromJar(File jarFile, String resourceName) {
        try (JarFile jar = new JarFile(jarFile)) {
            // Get the entry for the specified resource name
            JarEntry entry = jar.getJarEntry(resourceName);

            // If the entry exists, return its InputStream
            if (entry != null) {
                // Note: The InputStream returned by getInputStream is valid as long as the JarFile is open.
                // The try-with-resources on JarFile handles closing it when this method exits,
                // so the caller of findBootConfig needs to consume the InputStream immediately
                // or handle its lifecycle carefully if stored.
                return Config.readFromInputStream(jar.getInputStream(entry));
            }
        } catch (IOException e) {
            // Log or handle the exception if a specific JAR file cannot be opened/read
            // For this case, we'll just print a warning and return null, allowing the stream
            // processing in findBootConfig to continue with other JARs.
            System.err.println("Warning: Could not read JAR file " + jarFile.getAbsolutePath() + ": " + e.getMessage());
            // Return null if the resource is not found or an error occurs reading the JAR
        }
        return null; // Resource not found or error occurred
    }
}
