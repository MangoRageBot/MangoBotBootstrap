package org.mangorage.bootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        Path librariesDir = Paths.get("libraries");
        Path pluginsDir = Paths.get("plugins");

        // Step 1: Split libraries into modular and non-modular jars
        List<Path> libraryJars = Files.list(librariesDir)
                .filter(path -> path.toString().endsWith(".jar"))
                .collect(Collectors.toList());

        List<Path> modularJars = new ArrayList<>();
        List<Path> classpathJars = new ArrayList<>();

        for (Path jar : libraryJars) {
            if (isModularJar(jar)) {
                modularJars.add(jar);
            } else {
                classpathJars.add(jar);
            }
        }

        // Step 2: Create unnamed module loader for non-modular jars
        ClassLoader unnamedModuleLoader = createClassPathLoader(classpathJars);

        // Step 3: Create ModuleFinder for modular libraries
        ModuleFinder libFinder = ModuleFinder.of(modularJars.toArray(new Path[0]));
        checkDuplicates(libFinder);
        Set<String> libModules = getModuleNames(libFinder);

        // Step 4: Build layer for libraries
        Configuration libConfig = ModuleLayer.boot()
                .configuration()
                .resolve(libFinder, ModuleFinder.of(), libModules);

        ModuleLayer libLayer = ModuleLayer.boot()
                .defineModulesWithOneLoader(libConfig, unnamedModuleLoader);

        // Step 5: Plugins
        ModuleFinder pluginFinder = ModuleFinder.of(pluginsDir);
        checkDuplicates(pluginFinder);
        Set<String> pluginModules = getModuleNames(pluginFinder);

        Configuration pluginConfig = libLayer.configuration()
                .resolve(pluginFinder, ModuleFinder.of(), pluginModules);

        ModuleLayer pluginLayer = libLayer
                .defineModulesWithOneLoader(pluginConfig, unnamedModuleLoader);

        // Step 6: Load plugin main
        String pluginModule = "org.mangorage.mangobotcore";
        String entrypointClass = "org.mangorage.entrypoint.MangoBotCore";

        ClassLoader pluginLoader = pluginLayer.findLoader(pluginModule);
        Class<?> mainClass = pluginLoader.loadClass(entrypointClass);
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) new String[0]);

        System.out.println("Plugin executed successfully.");
    }

    private static boolean isModularJar(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile(), true)) {
            // Explicit module descriptor
            if (jar.getEntry("module-info.class") != null)
                return true;

            // Multi-release module-info
            if (jar.getEntry("META-INF/versions/9/module-info.class") != null)
                return true;

            // Check for Automatic-Module-Name in MANIFEST.MF
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                String autoModuleName = manifest.getMainAttributes().getValue("Automatic-Module-Name");
                return autoModuleName != null && !autoModuleName.isBlank();
            }

            return false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect JAR: " + jarPath, e);
        }
    }

    private static ClassLoader createClassPathLoader(List<Path> jars) {
        try {
            List<URL> urls = jars.stream()
                    .map(path -> {
                        try {
                            return path.toUri().toURL();
                        } catch (Exception e) {
                            throw new RuntimeException("Invalid jar: " + path, e);
                        }
                    })
                    .collect(Collectors.toList());

            return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create classpath loader", e);
        }
    }

    private static Set<String> getModuleNames(ModuleFinder finder) {
        return finder.findAll().stream()
                .map(ref -> ref.descriptor().name())
                .collect(Collectors.toSet());
    }

    private static void checkDuplicates(ModuleFinder finder) {
        Map<String, List<ModuleReference>> grouped = finder.findAll().stream()
                .collect(Collectors.groupingBy(ref -> ref.descriptor().name()));

        grouped.forEach((name, refs) -> {
            if (refs.size() > 1) {
                System.err.println("Duplicate module found: " + name);
                refs.forEach(ref -> System.err.println("  -> " + ref.location().orElse(null)));
                throw new RuntimeException("Duplicate modules detected: " + name);
            }
        });
    }

    public static void initOld(final String[] args) {
        URLClassLoader CL_libraries = new URLClassLoader(fetchJars(new File[]{new File("libraries")}), Thread.currentThread().getContextClassLoader().getParent());
        URLClassLoader cl = new URLClassLoader(fetchJars(new File[]{new File("plugins")}), CL_libraries);
        Thread.currentThread().setContextClassLoader(cl);
        callMain("org.mangorage.entrypoint.MangoBotCore", args, cl);
    }

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

    public static void callMain(String className, String[] args, ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
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
