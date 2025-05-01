package org.mangorage.bootstrap;

import java.io.IOException;
import java.lang.ModuleLayer;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet; // Added for HashSet
import java.util.List;
import java.util.Optional;
import java.util.Set; // Added for Set
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class NewBootstrap {

    public static void main(String[] args) throws Exception {
        Path librariesDir = Paths.get("libraries");
        Path pluginsDir = Paths.get("plugins");

        // Ensure directories exist
        ensureDirectoryExists(librariesDir);
        ensureDirectoryExists(pluginsDir);

        // --- Step 1: Prepare Library Paths ---
        List<Path> libraryJarPaths = listJarFiles(librariesDir);
        if (libraryJarPaths.isEmpty()) {
            System.out.println("No library JARs found in " + librariesDir.toAbsolutePath());
            // Decide if this is acceptable or an error
        }

        // --- FIX START: Explicitly define problematic Library JARs to exclude from ModuleFinder ---
        // List JARs that should be treated as non-modular classpath JARs ONLY,
        // even if they are in the libraries directory.
        // Add 'libraries/luaj-jse-3.0.1.jar' as it caused the InvalidModuleDescriptorException.
        Set<Path> nonModularOnlyLibJars = new HashSet<>();
        nonModularOnlyLibJars.add(librariesDir.resolve("luaj-jse-3.0.1.jar"));
        // Add any other Library JARs here that cause similar module descriptor errors.
        // nonModularOnlyLibJars.add(librariesDir.resolve("another-problematic-lib.jar"));

        List<Path> modularCandidateLibJarPaths = libraryJarPaths.stream()
                .filter(p -> !nonModularOnlyLibJars.contains(p))
                .collect(Collectors.toList());

        nonModularOnlyLibJars.forEach(p -> {
            if (libraryJarPaths.contains(p)) {
                System.out.println("Explicitly excluding library " + p.getFileName() + " from Library ModuleFinder.");
            } else {
                System.err.println("Warning: Problematic Library JAR listed for exclusion (" + p.getFileName() + ") was not found in the libraries directory.");
            }
        });
        // --- FIX END ---


        // --- Step 2: Create Library ClassLoader ---
        // Simple approach: Put ALL library JARs (including the problematic ones) into one ClassLoader.
        // Modular JARs will be found by the ModuleFinder later.
        // Non-modular JARs will be on the classpath (unnamed module) of this loader.
        ClassLoader libraryClassLoader = createUrlClassLoader("LibraryLoader", libraryJarPaths, ClassLoader.getSystemClassLoader()); // Use the original list
        System.out.println("Library ClassLoader parent: " + (libraryClassLoader.getParent() != null ? libraryClassLoader.getParent().getName() : "null"));


        // --- Step 3: Find Library Modules ---
        // This finder scans ONLY the candidate modular library JARs.
        ModuleFinder libFinder = ModuleFinder.of(modularCandidateLibJarPaths.toArray(Path[]::new)); // Use the filtered list
        Set<ModuleReference> libModuleRefs = libFinder.findAll(); // Should succeed now
        Set<String> libModuleNames = getModuleNames(libModuleRefs);
        System.out.println("Found Library Modules: " + libModuleNames);


        // --- Step 4: Define Library Layer ---
        // Resolve the found library modules against the boot layer.
        Configuration libConfig = ModuleLayer.boot()
                .configuration()
                .resolve(libFinder, ModuleFinder.of(), libModuleNames);

        // Define the layer using the resolved configuration and the library classloader.
        ModuleLayer libLayer = ModuleLayer.boot().defineModulesWithOneLoader(libConfig, libraryClassLoader);
        System.out.println("Library Layer Defined.");


        // --- Step 5: Prepare Plugin Paths ---
        List<Path> pluginJarPaths = listJarFiles(pluginsDir);
        if (pluginJarPaths.isEmpty()) {
            System.out.println("No plugins found in " + pluginsDir.toAbsolutePath() + ". Exiting.");
            return; // Exit if no plugins are present (adjust as needed)
        }

        // --- Step 6: Create Plugin ClassLoader ---
        // Simple approach: Put ALL plugin JARs into one ClassLoader.
        // Its parent is the Library ClassLoader, allowing plugins to see libraries (both modular and non-modular).
        ClassLoader pluginClassLoader = createUrlClassLoader("PluginLoader", pluginJarPaths, libraryClassLoader);
        System.out.println("Plugin ClassLoader parent: " + (pluginClassLoader.getParent() != null ? pluginClassLoader.getParent().getName() : "null"));


        // --- Step 7: Find Plugin Modules ---
        // This finder scans all plugin JARs to find explicit or automatic modules.
        // If any plugin JARs cause InvalidModuleDescriptorException, you might need
        // to add similar exclusion logic for plugins as done for libraries.
        ModuleFinder pluginFinder = ModuleFinder.of(pluginJarPaths.toArray(Path[]::new));
        Set<ModuleReference> pluginModuleRefs = pluginFinder.findAll();
        Set<String> pluginModuleNames = getModuleNames(pluginModuleRefs);
        System.out.println("Found Plugin Modules: " + pluginModuleNames);


        // --- Step 8: Resolve Plugin Configuration ---
        // Resolve plugin modules using the library layer's configuration as the parent.
        Configuration pluginConfig;
        try {
            pluginConfig = libLayer.configuration()
                    .resolve(pluginFinder, ModuleFinder.of(), pluginModuleNames);
        } catch (Exception e) {
            System.err.println("Failed to resolve plugin module dependencies. Check plugin requirements and library modules.");
            e.printStackTrace();
            return;
        }


        // --- Step 9: Define Plugin Layer ---
        // Define the plugin layer using its resolved configuration and the plugin classloader.
        // It is layered ON TOP of the library layer.
        ModuleLayer pluginLayer = libLayer.defineModulesWithManyLoaders(pluginConfig, pluginClassLoader);
        System.out.println("Plugin Layer Defined.");


        // --- Step 10: Load and Execute Target Plugin Main Class ---
        // Hardcoded for the example, replace with dynamic discovery if needed
        String targetPluginModule = "org.mangorage.mangobotcore"; // Example module name
        String entrypointClass = "org.mangorage.entrypoint.MangoBotCore"; // Example entry point

        Optional<Module> pluginModuleOptional = pluginLayer.findModule(targetPluginModule);

        if (pluginModuleOptional.isEmpty()) {
            System.err.println("Target plugin module '" + targetPluginModule + "' not found in the plugin layer.");
            System.err.println("Available plugin modules: " + pluginLayer.modules().stream().map(Module::getName).collect(Collectors.toSet()));
            // If the entry point is in a non-modular JAR, you'd load it directly
            // from the pluginClassLoader here using Class.forName(entrypointClass, true, pluginClassLoader);
            // and invoke its main method if applicable.
            return;
        }

        // Use the specific ClassLoader associated with the target plugin module
        // In this simple setup, it will be the main pluginClassLoader.
        ClassLoader targetPluginModuleLoader = pluginModuleOptional.get().getClassLoader();
        System.out.println("Target Plugin Module Loader: " + targetPluginModuleLoader.getName());


        // Set the ContextClassLoader - useful for many libraries
        Thread currentThread = Thread.currentThread();
        ClassLoader originalContextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(pluginClassLoader);

        try {
            // Load the main plugin class using its specific ClassLoader

            Class<?> mainClass = Class.forName(entrypointClass, true, targetPluginModuleLoader);
            System.out.println("Loaded main plugin class: " + mainClass.getName() + " using loader: " + mainClass.getClassLoader().getName());

            // Find and invoke the main method
            Method mainMethod = mainClass.getMethod("main", String[].class);

            System.out.println("Invoking main method on " + entrypointClass + " in module " + targetPluginModule);
            mainMethod.invoke(null, (Object) new String[0]); // Pass empty args array
            System.out.println("Plugin execution finished successfully.");

        } catch (ClassNotFoundException e) {
            System.err.println("Entry point class not found: " + entrypointClass + " using classloader for module " + targetPluginModule);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.err.println("main(String[]) method not found in class: " + entrypointClass);
            e.printStackTrace();
        } catch (ReflectiveOperationException e) { // Catch broader reflection issues
            System.err.println("Error invoking main method in " + entrypointClass);
            e.printStackTrace(); // Often holds the actual exception from the plugin's main method
            if (e.getCause() != null) {
                System.err.println("Caused by:");
                e.getCause().printStackTrace();
            }
        } catch (Exception e) { // Catch any other unexpected errors
            System.err.println("An unexpected error occurred during plugin execution.");
            e.printStackTrace();
        } finally {
            // Restore original TCCL
            currentThread.setContextClassLoader(originalContextClassLoader);
            System.out.println("Restored original Thread Context ClassLoader.");
        }
    }

    // Helper to ensure a directory exists
    private static void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            System.out.println("Directory not found, creating: " + directory.toAbsolutePath());
            Files.createDirectories(directory);
        } else if (!Files.isDirectory(directory)) {
            throw new IOException("Path exists but is not a directory: " + directory.toAbsolutePath());
        }
    }

    // Helper to list JAR files in a directory
    private static List<Path> listJarFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            System.err.println("Warning: Directory not found or not a directory: " + directory.toAbsolutePath());
            return List.of(); // Return empty list
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(path -> path.toString().toLowerCase().endsWith(".jar") && Files.isRegularFile(path))
                    .collect(Collectors.toList());
        }
    }

    // Helper to create a URLClassLoader with a name (for debugging)
    private static URLClassLoader createUrlClassLoader(String name, List<Path> jars, ClassLoader parent) {
        URL[] urls = jars.stream()
                .map(path -> {
                    try {
                        return path.toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException("Invalid JAR path URI: " + path, e);
                    }
                })
                .toArray(URL[]::new);

        // Create the URLClassLoader
        System.out.println("Creating URLClassLoader '" + name + "' with " + jars.size() + " JARs.");
        return new URLClassLoader(name, urls, parent);
    }

    // Get module names from references
    private static Set<String> getModuleNames(Set<ModuleReference> refs) {
        return refs.stream()
                .map(ref -> ref.descriptor().name())
                .collect(Collectors.toSet());
    }
}
