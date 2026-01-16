package org.mangorage.bootstrap.api.module;

import org.mangorage.bootstrap.internal.launch.ModuleLayerImpl;

import java.util.List;

public sealed interface IModuleLayer permits ModuleLayerImpl {
    void addOpens(String sourceModule, String pkg, String targetModule);
    void addExports(String sourceModule, String pkg, String targetModule);
    void addReads(String sourceModule, String targetModule);

    default void addOpens(String sourceModule, List<String> packages, List<String> targetModules) {
        targetModules.forEach(target -> {
            packages.forEach(pkg -> {
                addOpens(sourceModule, pkg, target);
            });
        });
    }

    default void addExports(String sourceModule, List<String> packages, List<String> targetModules) {
        targetModules.forEach(target -> {
            packages.forEach(pkg -> {
                addExports(sourceModule, pkg, target);
            });
        });
    }
}
