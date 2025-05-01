package org.mangorage.bootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.mangorage.bootstrap.Bootstrap.fetchJars;

public final class Test {
    public static void main(String[] args) throws IOException {
        try {
            System.out.println("you have 15 seconds!");
            Thread.sleep(15_000);

        } catch (Throwable ignored) {}

        LibraryHandler.handle();

        Path libsPath = Path.of("sortedLibraries");
        Path pluginsPath = Path.of("plugins");

        ModuleFinder plugins = ModuleFinder.of(pluginsPath);

        ModuleFinder finder = ModuleFinder.of(libsPath, pluginsPath);

        Configuration cfg = ModuleLayer.boot()
                .configuration()
                .resolveAndBind(
                        finder,
                        ModuleFinder.of(),
                        Set.of()
                );


        URLClassLoader CL_libraries = new URLClassLoader(fetchJars(new File[]{new File("libraries")}), Thread.currentThread().getContextClassLoader().getParent());
        URLClassLoader cl = new URLClassLoader(fetchJars(new File[]{new File("plugins")}), CL_libraries);
        Thread.currentThread().setContextClassLoader(cl);

        var layer = ModuleLayer.boot().defineModulesWithOneLoader(cfg, cl);
        callMain("org.mangorage.entrypoint.MangoBotCore", args, cl, layer);
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
