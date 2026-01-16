package org.mangorage.bootstrap.api.dependency;

import java.nio.file.Path;

public interface IDependency {
    String getName();
    ModuleNameOrigin getModuleNameOrigin();
    Path resolveJar();
}
