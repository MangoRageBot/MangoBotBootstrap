package org.mangorage.bootstrap;

import org.mangorage.bootstrap.internal.JarHandler;
import org.mangorage.bootstrap.internal.MangoLoader;
import org.mangorage.bootstrap.internal.Util;

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mangorage.bootstrap.internal.Util.*;

public final class Bootstrap {

    public static void main(String[] args) throws IOException {

        final var librariesPath = Path.of("libraries");
        final var pluginsPath = Path.of("plugins");

        JarHandler.safeHandle(Path.of("libraries"), Path.of("sorted-libraries"));

        List<Path> deleteFiles = List.of(
                Path.of("sorted-libraries").resolve("okio-jvm-3.6.0.jar")
        );

        for (Path deleteFile : deleteFiles) {
            Files.deleteIfExists(deleteFile);
        }

        final var moduleCfg = Configuration.resolve(
                ModuleFinder.of(pluginsPath),
                List.of(
                        ModuleLayer.boot().configuration()
                ),
                ModuleFinder.of(
                        Path.of("sorted-libraries")
                ),
                Util.getModuleNames(pluginsPath)
        );

        final var moduleCl = new MangoLoader(fetchJars(librariesPath, pluginsPath), moduleCfg.modules(), Thread.currentThread().getContextClassLoader());

        final var moduleLayerController = ModuleLayer.defineModules(moduleCfg, List.of(ModuleLayer.boot()), s -> moduleCl);
        final var moduleLayer = moduleLayerController.layer();

        Thread.currentThread().setContextClassLoader(moduleCl);

        moduleCl.loadTransformers();

        callMain("org.mangorage.entrypoint.MangoBotCore", args, moduleLayer.findModule("org.mangorage.mangobotcore").get());
    }
}
