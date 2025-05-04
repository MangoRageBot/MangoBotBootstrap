package org.mangorage.bootstrap;

import org.mangorage.bootstrap.internal.Util;

import java.io.IOException;
import java.nio.file.Path;

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

        final var moduleLayer = ModuleLayer.boot().defineModulesWithOneLoader(moduleCfg, cl);
        callMain(cfg.getMainClass(), args, cl, moduleLayer);
    }
}
