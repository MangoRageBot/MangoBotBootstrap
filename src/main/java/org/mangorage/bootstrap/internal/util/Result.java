package org.mangorage.bootstrap.internal.util;

import org.mangorage.bootstrap.internal.JarHandler;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public record Result(String name, ModuleNameOrigin origin, Path jar, AtomicReference<UnsafeRunnable> task) {
    public Result(String name, ModuleNameOrigin origin, Path jar) {
        this(name, origin, jar, new AtomicReference<>());
    }
}
