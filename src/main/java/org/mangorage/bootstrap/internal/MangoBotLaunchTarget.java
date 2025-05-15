package org.mangorage.bootstrap.internal;

import org.mangorage.bootstrap.api.launch.ILaunchTarget;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mangorage.bootstrap.internal.Util.callMain;

public final class MangoBotLaunchTarget implements ILaunchTarget {
    @Override
    public String getId() {
        return "mangobot";
    }

    @Override
    public void launch(ModuleLayer parent, String[] args) throws Throwable {
        final var librariesPath = Path.of("libraries");
        final var sortedLibrariesPath = Path.of("sorted-libraries");
        final var pluginsPath = Path.of("plugins");

        JarHandler.safeHandle(librariesPath, sortedLibrariesPath);

        List<Path> deleteFiles = List.of(
               sortedLibrariesPath.resolve("okio-3.6.0.jar")
        );

        for (Path deleteFile : deleteFiles) {
            Files.deleteIfExists(deleteFile);
        }

        Set<String> moduleNames = new HashSet<>();
        moduleNames.addAll(Util.getModuleNames(pluginsPath));
        moduleNames.addAll(Util.getModuleNames(sortedLibrariesPath));


        final var moduleCfg = Configuration.resolve(
                ModuleFinder.of(
                        sortedLibrariesPath
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

        addExports(
                moduleLayerController,
                moduleLayer.findModule("org.spongepowered.mixin"),
                moduleLayer.findModule("org.mangorage.mangobotmixin"),
                List.of(
                        "org.spongepowered.asm.mixin.transformer",
                        "org.spongepowered.asm.transformers"
                )
        );

        addOpens(
                moduleLayerController,
                moduleLayer.findModule("org.spongepowered.mixin"),
                moduleLayer.findModule("org.mangorage.mangobotmixin"),
                List.of(
                        "org.spongepowered.asm.mixin"
                )
        );

        // "--add-exports", "org.spongepowered.mixin/org.spongepowered.asm.mixin.transformer=org.mangorage.mangobotcore", "--add-opens", "org.spongepowered.mixin/org.spongepowered.asm.mixin.transformer=org.mangorage.mangobotcore"

        moduleCL.load();

        callMain("org.mangorage.entrypoint.MangoBotCore", args, moduleLayer.findModule("org.mangorage.mangobotcore").get());
    }

    static void addExports(ModuleLayer.Controller controller, Optional<Module> source, Optional<Module> target, List<String> packages) {
        if (source.isPresent() && target.isPresent()) {
            packages.forEach(pkg -> {
                controller.addExports(source.get(), pkg, target.get());
            });
        }
    }

    static void addOpens(ModuleLayer.Controller controller, Optional<Module> source, Optional<Module> target, List<String> packages) {
        if (source.isPresent() && target.isPresent()) {
            packages.forEach(pkg -> {
                controller.addOpens(source.get(), pkg, target.get());
            });
        }
    }
}
