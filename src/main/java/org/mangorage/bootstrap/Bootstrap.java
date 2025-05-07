package org.mangorage.bootstrap;

import org.mangorage.bootstrap.internal.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.mangorage.bootstrap.internal.Util.*;

public final class Bootstrap {

    public static void main(String[] args) throws IOException {
        final var cfgOptional = Util.findBootConfig(Path.of(""));

        if (!cfgOptional.isPresent()) {
            throw new IllegalStateException("Failed to find any boot.cfg from the root folder/sub folders");
        }

        final var cfg = cfgOptional.get();


        cfg.handleJars();

        final var moduleCfg = cfg.constructModuleConfiguration();

        final var cl = cfg.constructClassloaders();
        Thread.currentThread().setContextClassLoader(cl);

        final var moduleController = ModuleLayer.defineModules(moduleCfg, List.of(ModuleLayer.boot()), s -> {
            if (s.startsWith("org.mangorage") & !s.contains("scanner")) {
                return cl;
            } else {
                return cl.getParent();
            }
        });


//        moduleControllaer.layer().modules().forEach(moduleControllaer::enableNativeAccess);

        callMain(cfg.getMainClass(), args, moduleController.layer().findModule("org.mangorage.mangobotcore").get());
    }
}
