package org.mangorage.bootstrap.api.logging;

public interface ILoggerProvider {

    String getName();

    default IMangoLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    IMangoLogger getLogger(String name);
}
