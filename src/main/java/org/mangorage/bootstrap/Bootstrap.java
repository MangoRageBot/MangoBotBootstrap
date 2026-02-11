package org.mangorage.bootstrap;

import org.mangorage.bootstrap.api.launch.ILaunchTarget;
import org.mangorage.bootstrap.internal.util.Util;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Bootstrap orchestrator for modular applications.
 *
 * <p>This class manages the startup sequence by:
 * <ol>
 *   <li>Parsing and validating command-line arguments</li>
 *   <li>Constructing module layers with proper hierarchy</li>
 *   <li>Discovering launch targets via ServiceLoader</li>
 *   <li>Delegating execution to the selected target</li>
 * </ol>
 *
 * <p><strong>Usage:</strong> {@code java -m org.mangorage.bootstrap --launchTarget <targetId>}
 *
 * @since 1.0.84
 * @see ILaunchTarget
 */
public final class Bootstrap {

    private static final Logger LOGGER = Logger.getLogger(Bootstrap.class.getName());
    private static final String LAUNCH_TARGET_ARG = "--launchTarget";
    private static final String DEFAULT_LAUNCH_PATH = "launch";

    /**
     * Main entry point for the bootstrap framework.
     *
     * @param args Command line arguments. Must include --launchTarget followed by target ID.
     * @throws IllegalArgumentException if arguments are invalid
     * @throws IllegalStateException if launch target cannot be found or executed
     */
    public static void main(String[] args) throws Throwable {
        LOGGER.info("Starting MangoBotBootstrap framework");

        validateArguments(args);

        final String launchTarget = args[1];
        validateLaunchTarget(launchTarget);

        LOGGER.info("Initializing module layers for launch target: " + launchTarget);

        ModuleLayer parent = getParentModuleLayer();
        Path launchPath = Path.of(DEFAULT_LAUNCH_PATH);

        final ModuleLayer moduleLayer = createLaunchModuleLayer(parent, launchPath);
        final Map<String, ILaunchTarget> launchTargetMap = discoverLaunchTargets(moduleLayer);

        if (!launchTargetMap.containsKey(launchTarget)) {
            throw new IllegalStateException(
                    String.format("Launch target '%s' not found. Available targets: %s",
                            launchTarget, launchTargetMap.keySet()));
        }

        LOGGER.info("Loading BootstrapLifecycle hooks");
        final var lifecycleHooks = ServiceLoader.load(moduleLayer, org.mangorage.bootstrap.api.lifecycle.IBootstrapLifecycle.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();

        LOGGER.info("Launching target: " + launchTarget);

        try {
            launchTargetMap.get(launchTarget).launch(moduleLayer, parent, args);
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Error during launch target execution: " + launchTarget, t);
            lifecycleHooks.forEach(hook -> hook.onError(t, moduleLayer));
            throw t;
        }

        LOGGER.info("Bootstrap completed successfully");
    }

    /**
     * Validates command-line arguments format and content.
     */
    private static void validateArguments(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException(
                    "Missing required arguments. Usage: --launchTarget <targetId>");
        }

        if (!LAUNCH_TARGET_ARG.equals(args[0])) {
            throw new IllegalArgumentException(
                    String.format("First argument must be '%s', got: %s", LAUNCH_TARGET_ARG, args[0]));
        }
    }

    /**
     * Validates the launch target identifier format.
     */
    private static void validateLaunchTarget(String target) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("Launch target cannot be null or empty");
        }

        if (!target.matches("[a-zA-Z0-9._-]+")) {
            throw new IllegalArgumentException(
                    String.format("Invalid launch target format: '%s'. Must contain only letters, numbers, dots, underscores, and hyphens", target));
        }
    }

    /**
     * Determines the appropriate parent module layer.
     */
    private static ModuleLayer getParentModuleLayer() {
        ModuleLayer parent = null;

        if (Bootstrap.class.getModule() != null) {
            parent = Bootstrap.class.getModule().getLayer();
        }

        return parent != null ? parent : ModuleLayer.boot();
    }

    /**
     * Creates the launch module layer from the specified path.
     */
    private static ModuleLayer createLaunchModuleLayer(ModuleLayer parent, Path launchPath) {
        try {
            final Configuration moduleCfg = Configuration.resolveAndBind(
                    ModuleFinder.of(launchPath),
                    List.of(parent.configuration()),
                    ModuleFinder.of(),
                    Files.exists(launchPath) ? Util.getModuleNames(launchPath) : Set.of()
            );

            final ModuleLayer.Controller moduleLayerController = ModuleLayer.defineModulesWithOneLoader(
                    moduleCfg,
                    List.of(parent),
                    Thread.currentThread().getContextClassLoader()
            );

            LOGGER.fine("Successfully created module layer with " + moduleCfg.modules().size() + " modules");
            return moduleLayerController.layer();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create module layer from path: " + launchPath, e);
            throw new IllegalStateException("Module layer creation failed", e);
        }
    }

    /**
     * Discovers all available launch targets in the module layer.
     */
    private static Map<String, ILaunchTarget> discoverLaunchTargets(ModuleLayer moduleLayer) {
        final Map<String, ILaunchTarget> launchTargetMap = new HashMap<>();

        try {
            ServiceLoader.load(moduleLayer, ILaunchTarget.class)
                    .stream()
                    .forEach(provider -> {
                        try {
                            final ILaunchTarget target = provider.get();
                            final String targetId = target.getId();

                            if (targetId == null || targetId.trim().isEmpty()) {
                                LOGGER.warning("Ignoring launch target with null or empty ID from provider: " + provider.type());
                                return;
                            }

                            if (launchTargetMap.containsKey(targetId)) {
                                LOGGER.warning("Duplicate launch target ID detected: " + targetId + ". Using first occurrence.");
                                return;
                            }

                            launchTargetMap.put(targetId, target);
                            LOGGER.fine("Discovered launch target: " + targetId + " (" + provider.type() + ")");

                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to load launch target provider: " + provider.type(), e);
                        }
                    });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to discover launch targets", e);
            throw new IllegalStateException("Launch target discovery failed", e);
        }

        LOGGER.info("Discovered " + launchTargetMap.size() + " launch targets: " + launchTargetMap.keySet());
        return launchTargetMap;
    }
}