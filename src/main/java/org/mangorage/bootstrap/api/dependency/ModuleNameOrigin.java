package org.mangorage.bootstrap.api.dependency;

public enum ModuleNameOrigin {
    MODULE_INFO, // HIGHEST
    MULTI_RELEASE, // HIGH
    MANIFEST, // MEDIUM
    MANIFEST_BUNDLE_SYMBOLIC_NAME, // LOW
    MODULE_FINDER, // LOWEST
    GUESSED // TOTAL LOWEST
}
