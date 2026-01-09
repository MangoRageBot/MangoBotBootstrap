package org.mangorage.bootstrap.internal.util;

public enum ModuleNameOrigin {
    MODULE_INFO, // Takes Highest priority
    MANIFEST,
    MANIFEST_BUNDLE_SYMBOLIC_NAME,
    MULTI_RELEASE,
    GUESSED // Takes lowest
}
