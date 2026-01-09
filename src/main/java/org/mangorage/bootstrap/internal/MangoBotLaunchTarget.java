package org.mangorage.bootstrap.internal;

import org.mangorage.bootstrap.api.launch.ILaunchTarget;
import org.mangorage.bootstrap.internal.util.Result;

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mangorage.bootstrap.internal.Util.callMain;

public final class MangoBotLaunchTarget implements ILaunchTarget {

    /**
     * Deletes the target directory if it exists, then copies all files from the list into it.
     *
     * @param files      List of files to copy
     * @param targetDir  Directory to copy files into
     * @throws IOException if anything goes wrong
     */
    public static void copyFilesToDirectory(List<Path> files, Path targetDir) throws IOException {
        // Delete directory if it exists
        if (Files.exists(targetDir)) {
            deleteDirectoryRecursively(targetDir);
        }

        // Recreate the empty directory
        Files.createDirectories(targetDir);

        // Copy each file
        for (Path file : files) {
            if (!Files.isRegularFile(file)) continue; // skip garbage
            Path dest = targetDir.resolve(file.getFileName());
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteDirectoryRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;

        // Walk the directory bottom-up and delete everything
        Files.walk(dir)
                .sorted((a, b) -> b.compareTo(a)) // delete children first
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete " + path, e);
                    }
                });
    }

    @Override
    public String getId() {
        return "mangobot";
    }

    @Override
    public void launch(ModuleLayer parent, String[] args) throws Throwable {
        final var librariesPath = Path.of("libraries");
        final var pluginsPath = Path.of("plugins");


        final Map<String, List<Result>> dependencies = DependencyHandler.scanPackages(pluginsPath.toAbsolutePath(), librariesPath.toAbsolutePath());
        final Map<String, Result> finalDependencies = new HashMap<>();

        dependencies.forEach((id, results) -> {
            final Result bestResult = results.stream()
                    .min(Comparator.comparingInt(r -> r.origin().ordinal()))
                    .get();
            finalDependencies.put(bestResult.name(), bestResult);
        });

        Path sortedLibraries = Path.of("sorted-libraries").toAbsolutePath();
        final var list = finalDependencies.entrySet()
                .stream()
                .map(entry -> entry.getValue().jar())
                .toList();

        copyFilesToDirectory(list, sortedLibraries);

        Set<String> moduleNames = new HashSet<>();
        moduleNames.addAll(Util.getModuleNames(pluginsPath));
        moduleNames.addAll(Util.getModuleNames(sortedLibraries));

        moduleNames.remove("kotlin-stdlib-common");

        final var moduleCfg = Configuration.resolve(
                ModuleFinder.of(
                        sortedLibraries
                ),
                List.of(
                        parent.configuration()
                ),
                ModuleFinder.of(
                        pluginsPath
                ),
                moduleNames
        );

        final var moduleCL = new MangoLoaderImpl(moduleCfg.modules(), Thread.currentThread().getContextClassLoader());

        final var moduleLayerController = ModuleLayer.defineModules(moduleCfg, List.of(parent), s -> moduleCL);
        final var moduleLayer = moduleLayerController.layer();

        Thread.currentThread().setContextClassLoader(moduleCL);

        moduleCL.load(moduleLayer, moduleLayerController);

        callMain("org.mangorage.entrypoint.MangoBotCore", args, moduleLayer.findModule("org.mangorage.mangobotcore").get());
    }
}
