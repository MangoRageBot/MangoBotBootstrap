package org.mangorage.bootstrap;

import org.mangorage.bootstrap.api.launch.ILaunchTarget;
import org.mangorage.bootstrap.internal.Util;
import org.mangorage.bootstrap.internal.impl.MangoBotLaunchTarget;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public final class Bootstrap {

    public static void main(String[] args) throws Throwable {
        if (!(args.length >= 2)) {
            throw new IllegalStateException("Need to define a launchTarget, --launchTarget mangobot");
        }

        if (!args[0].equals("--launchTarget")) {
            throw new IllegalStateException("Need to have --launchTarget be defined first...");
        }

        final var launchTarget = args[1];

        ModuleLayer parent = null;

        if (Bootstrap.class.getModule() != null) {
            parent = Bootstrap.class.getModule().getLayer();
        } else {
            parent = ModuleLayer.boot(); // We dont have a module for Bootstrap..., so assume we are using the boot layer...
        }

        // Where additional launch targets can be defined...
        Path launchPath = Path.of(
                "launch"
        );

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

        final Map<String, ILaunchTarget> launchTargetMap = new HashMap<>();

        ServiceLoader.load(moduleLayer, ILaunchTarget.class)
                .stream()
                .forEach(provider -> {
                    final var target = provider.get();
                    launchTargetMap.put(target.getId(), target);
                });

        // Only add if we dont have any other launch targets...
        if (launchTargetMap.isEmpty()) {
            final var defaultLaunchTarget = new MangoBotLaunchTarget();
            launchTargetMap.put(defaultLaunchTarget.getId(), defaultLaunchTarget);
        }

        if (!launchTargetMap.containsKey(launchTarget)) {
            throw new IllegalStateException("Cant find launch target '%s'".formatted(launchTarget));
        }

        launchTargetMap.get(launchTarget).launch(parent, args);
    }
}
