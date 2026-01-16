package org.mangorage.bootstrap;

import com.google.gson.Gson;
import org.mangorage.bootstrap.api.launch.ILaunchTarget;
import org.mangorage.bootstrap.internal.util.Util;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public final class Bootstrap {

    private static final Gson GSON = new Gson();

    public static void main(String[] args) throws Throwable {
        if (!(args.length >= 2)) {
            throw new IllegalStateException("Need to define a launchTarget, --launchTarget mangobot");
        }

        if (!args[0].equals("--launchTarget")) {
            throw new IllegalStateException("Need to have --launchTarget be defined first...");
        }

        final var launchTarget = args[1];

        ModuleLayer parent = null;

        if (parent == null)
            parent = ModuleLayer.boot();

        // Where additional launch targets can be defined...
        final Path launchPath = Path.of("boot").toAbsolutePath();

        final var moduleCfg = Configuration
                .resolveAndBind(
                        ModuleFinder.of(
                                launchPath
                        ),
                        List.of(
                                parent.configuration()
                        ),
                        ModuleFinder.of(),
                        Files.exists(launchPath) ?
                        Util.getModuleNames(
                                launchPath
                        ) : Set.of()
                );

        final var moduleLayerController = ModuleLayer.defineModulesWithOneLoader(
                moduleCfg,
                List.of(parent),
                Thread.currentThread().getContextClassLoader()
        );
        final var moduleLayer = moduleLayerController.layer();
        final var moduleCL = new URLClassLoader(
                new URL[]{
                        Path.of("boot/boot.jar").toUri().toURL()
                },
                Thread.currentThread().getContextClassLoader()
        );

        moduleLayer.defineModulesWithOneLoader(moduleCfg, moduleCL);
        Thread.currentThread().setContextClassLoader(moduleCL);

        final Map<String, ILaunchTarget> launchTargetMap = new HashMap<>();

        ServiceLoader.load(moduleLayer, ILaunchTarget.class)
                .stream()
                .forEach(provider -> {
                    try {
                        final var target = provider.get();
                        launchTargetMap.put(target.getId(), target);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });

        if (!launchTargetMap.containsKey(launchTarget)) {
            throw new IllegalStateException("Cant find launch target '%s'".formatted(launchTarget));
        }

        launchTargetMap.get(launchTarget).launch(moduleLayer, parent, args);
    }
}
