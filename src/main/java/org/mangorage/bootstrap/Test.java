package org.mangorage.bootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mangorage.bootstrap.Bootstrap.fetchJars;

public final class Test {
    public static void main(String[] args) throws IOException {
        try {
            System.out.println("you have 15 seconds!");


        } catch (Throwable ignored) {}

        final var list = LibraryHandler.handle();

        Path libsPath = Path.of("sortedLibraries");
        Path pluginsPath = Path.of("plugins");

        Files.deleteIfExists(libsPath.resolve("okio-jvm-3.6.0.jar"));

        ModuleFinder plugins = ModuleFinder.of(pluginsPath);

        ModuleFinder finder = ModuleFinder.of(libsPath, pluginsPath);

        Configuration cfg = ModuleLayer.boot()
                .configuration()
                .resolveAndBind(
                        finder,
                        ModuleFinder.of(),
                        getModuleNames(pluginsPath)
                );


        URLClassLoader CL_libraries = new URLClassLoader(fetchJars(new File[]{new File("libraries")}), Thread.currentThread().getContextClassLoader().getParent());
        URLClassLoader cl = new URLClassLoader(fetchJars(new File[]{new File("plugins")}), CL_libraries);
        Thread.currentThread().setContextClassLoader(cl);

        var layer = ModuleLayer.boot().defineModulesWithOneLoader(cfg, cl);
        callMain("org.mangorage.entrypoint.MangoBotCore", args, cl, layer);
    }

    public static Set<String> getModuleNames(Path folder) {
        Set<String> moduleNames = new HashSet<>();

        if (folder == null || !folder.toFile().isDirectory()) {
            throw new IllegalArgumentException("That's not a valid folder, genius: " + folder);
        }

        File[] jarFiles = folder.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) return moduleNames;

        for (File jar : jarFiles) {
            try {
                ModuleFinder finder = ModuleFinder.of(jar.toPath());
                Set<ModuleReference> modules = finder.findAll();

                for (ModuleReference moduleRef : modules) {
                    var descriptor = moduleRef.descriptor();
                    moduleNames.add(descriptor.name());
                }
            } catch (Exception e) {
                System.err.println("Couldn't process " + jar.getName() + ": " + e.getMessage());
            }
        }

        return moduleNames;
    }

    public static void callMain(String className, String[] args, ClassLoader classLoader, ModuleLayer moduleLayer) {
        try {
            Class<?> clazz = Class.forName(moduleLayer.findModule("org.mangorage.mangobotcore").get(), className);
            Method mainMethod = clazz.getMethod("main", String[].class);

            // Make sure it's static and public
            if (!java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers())) {
                throw new IllegalStateException("Main method is not static, are you high?");
            }

            // Invoke the main method with a godawful cast
            mainMethod.invoke(null, (Object) args);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't reflectively call main because something exploded.", e);
        }
    }
}
