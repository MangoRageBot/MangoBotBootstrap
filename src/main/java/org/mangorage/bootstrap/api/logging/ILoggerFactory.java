package org.mangorage.bootstrap.api.logging;

import org.mangorage.bootstrap.internal.logger.DefaultLoggerFactory;
import org.mangorage.bootstrap.internal.logger.DeferredMangoLogger;

public interface ILoggerFactory {
    static ILoggerFactory getDefault() {
        return DefaultLoggerFactory.INSTANCE;
    }

    /**
     * Gets the logger provider by its name. The name is defined by the provider itself
     * and can be used to differentiate between multiple providers.
     */
    ILoggerProvider getProvider(String providerName);

    /**
     * Gets the wrapped logger provider by its name. This is used to get the underlying logger provider
     * Sometimes providers come in later then originally requested, so this method can be used to use the provider after it has been loaded.
     */
    default IDeferredMangoLogger getWrappedProvider(String providerName, Class<?> clazz) {
        return new DeferredMangoLogger(providerName, provider -> provider.getLogger(clazz));
    }

    /**
     * Gets the wrapped logger provider by its name. This is used to get the underlying logger provider
     * Sometimes providers come in later then originally requested, so this method can be used to use the provider after it has been loaded.
     */
    default IDeferredMangoLogger getWrappedProvider(String providerName, String name) {
        return new DeferredMangoLogger(providerName, provider -> provider.getLogger(name));
    }
}
