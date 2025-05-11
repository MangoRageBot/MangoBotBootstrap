package org.mangorage.bootstrap.internal.impl;

import org.mangorage.bootstrap.api.launch.ILaunchTarget;
import org.mangorage.bootstrap.internal.JarHandler;
import org.mangorage.bootstrap.internal.MangoLoader;
import org.mangorage.bootstrap.internal.Util;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mangorage.bootstrap.internal.Util.callMain;

public final class MangoBotLaunchTarget implements ILaunchTarget {
    @Override
    public String getId() {
        return "mangobot";
    }

    @Override
    public void launch(ModuleLayer parent, String[] args) throws Throwable {
        final var librariesPath = Path.of("libraries");
        final var sortedLibraries = Path.of("sorted-libraries");
        final var pluginsPath = Path.of("plugins");

        JarHandler.safeHandle(librariesPath, sortedLibraries);

        List<Path> deleteFiles = List.of(
               sortedLibraries.resolve("okio-3.6.0.jar")
        );

        for (Path deleteFile : deleteFiles) {
            Files.deleteIfExists(deleteFile);
        }

        final var moduleLibrariesCfg = Configuration.resolve(
                ModuleFinder.of(sortedLibraries),
                List.of(
                        parent.configuration()
                ),
                ModuleFinder.of(),
                Util.getModuleNames(
                        sortedLibraries
                )
        );

        final var moduleLibrariesCL = new MangoLoader(moduleLibrariesCfg.modules(), Thread.currentThread().getContextClassLoader());

        final var moduleLibrariesLayerController = ModuleLayer.defineModules(moduleLibrariesCfg, List.of(parent), s -> moduleLibrariesCL);
        final var moduleLibrariesLayer = moduleLibrariesLayerController.layer();



        final var modulePluginsCfg = Configuration.resolve(
                ModuleFinder.of(pluginsPath),
                List.of(
                        moduleLibrariesLayer.configuration()
                ),
                ModuleFinder.of(),
                Util.getModuleNames(
                        pluginsPath
                )
        );

        final var modulePluginsCL = new MangoLoader(modulePluginsCfg.modules(), moduleLibrariesCL);

        final var modulePluginsLayerController = ModuleLayer.defineModules(modulePluginsCfg, List.of(moduleLibrariesLayer), s -> modulePluginsCL);
        final var modulePluginsLayer = modulePluginsLayerController.layer();

        Thread.currentThread().setContextClassLoader(modulePluginsCL);

        moduleLibrariesCL.load();
        modulePluginsCL.load();

        callMain("org.mangorage.entrypoint.MangoBotCore", args, modulePluginsLayer.findModule("org.mangorage.mangobotcore").get());
    }
}
