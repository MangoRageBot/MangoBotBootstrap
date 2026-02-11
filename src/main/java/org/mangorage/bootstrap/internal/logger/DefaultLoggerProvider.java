package org.mangorage.bootstrap.internal.logger;

import org.mangorage.bootstrap.api.logging.AbstractLoggerProvider;
import org.mangorage.bootstrap.api.logging.ILoggerProvider;
import org.mangorage.bootstrap.api.logging.IMangoLogger;

public final class DefaultLoggerProvider extends AbstractLoggerProvider {
    public static final ILoggerProvider INSTANCE = new DefaultLoggerProvider("default");

    DefaultLoggerProvider(String name) {
        super(name);
    }

    @Override
    protected IMangoLogger createLogger(String name) {
        return new JulMangoLogger(name);
    }
}
