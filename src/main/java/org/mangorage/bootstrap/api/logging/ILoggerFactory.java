package org.mangorage.bootstrap.api.logging;

import org.mangorage.bootstrap.internal.logger.DefaultLoggerFactory;

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
     * Checks if a logger provider with the given name is available. The name is defined by the provider itself
     * and can be used to differentiate between multiple providers.
     */
    boolean hasProvider(String providerName);

    /**
     * Gets the wrapped logger provider by its name. This is used to get the underlying logger provider
     * Sometimes providers come in later then originally requested, so this method can be used to use the provider after it has been loaded.
     */
    IDeferredMangoLogger getWrappedProvider(String providerName, Class<?> clazz);

    /**
     * Gets the wrapped logger provider by its name. This is used to get the underlying logger provider
     * Sometimes providers come in later then originally requested, so this method can be used to use the provider after it has been loaded.
     */
    IDeferredMangoLogger getWrappedProvider(String providerName, String name);
}
