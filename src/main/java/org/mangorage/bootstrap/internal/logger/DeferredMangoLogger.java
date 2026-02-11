package org.mangorage.bootstrap.internal.logger;

import org.mangorage.bootstrap.api.logging.IDeferredMangoLogger;
import org.mangorage.bootstrap.api.logging.ILoggerFactory;
import org.mangorage.bootstrap.api.logging.ILoggerProvider;
import org.mangorage.bootstrap.api.logging.IMangoLogger;

import java.util.function.Function;

public final class DeferredMangoLogger implements IDeferredMangoLogger {

    private final String provider;
    private final Function<ILoggerProvider, IMangoLogger> loggerFunction;
    private volatile IMangoLogger logger;

    public DeferredMangoLogger(String provider, Function<ILoggerProvider, IMangoLogger> loggerFunction) {
        this.provider = provider;
        this.loggerFunction = loggerFunction;
    }


    @Override
    public IMangoLogger get() {
        if (logger == null) {
            synchronized (this) {
                if (logger == null) {
                    ILoggerProvider loggerProvider = ILoggerFactory.getDefault().getProvider(provider);
                    logger = loggerFunction.apply(loggerProvider);
                    if (logger != null) return logger;
                }
            }
        } else {
            return logger;
        }
        return loggerFunction.apply(ILoggerFactory.getDefault().getProvider("default"));
    }
}
