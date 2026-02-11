package org.mangorage.bootstrap.internal.logger;

import org.mangorage.bootstrap.api.logging.ILoggerFactory;
import org.mangorage.bootstrap.api.logging.ILoggerProvider;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultLoggerFactory implements ILoggerFactory {
    public static final DefaultLoggerFactory INSTANCE = new DefaultLoggerFactory();
    private static final Map<String, ILoggerProvider> providers = new ConcurrentHashMap<>(); // Placeholder for actual provider storage

    static {
        providers.put("default", DefaultLoggerProvider.INSTANCE);
    }

    public static void load(ModuleLayer moduleLayer) {
        ServiceLoader.load(moduleLayer, ILoggerProvider.class).forEach(provider -> {
            // Register the provider in some way, e.g., add it to a map
            providers.put(provider.getName(), provider);
        });
    }

    DefaultLoggerFactory() {
    }

    @Override
    public ILoggerProvider getProvider(String providerName) {
        return providers.getOrDefault(providerName, DefaultLoggerProvider.INSTANCE);
    }

    @Override
    public boolean hasProvider(String providerName) {
        return providers.containsKey(providerName);
    }
}
