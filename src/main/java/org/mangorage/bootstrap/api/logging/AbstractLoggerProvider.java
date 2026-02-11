package org.mangorage.bootstrap.api.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractLoggerProvider implements ILoggerProvider {
    private final String name;
    private final Map<String, IMangoLogger> loggerCache = new ConcurrentHashMap<>();

    public AbstractLoggerProvider(String name) {
        this.name = name;
    }

    protected abstract IMangoLogger createLogger(String name);

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IMangoLogger getLogger(String name) {
        synchronized (this.name) {
            if (!loggerCache.containsKey(name)) {
                final var logger = createLogger(name);
                loggerCache.put(name, logger);
                return logger;
            }
        }
        return loggerCache.get(name);
    }
}
