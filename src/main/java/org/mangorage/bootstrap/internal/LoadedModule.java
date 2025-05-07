package org.mangorage.bootstrap.internal;

import java.io.IOException;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.security.CodeSigner;
import java.security.CodeSource;

public final class LoadedModule {
    private final ModuleReference moduleReference;
    private final ModuleReader moduleReader;
    private final CodeSource codeSource;

    LoadedModule(ModuleReference moduleReference) throws IOException {
        this.moduleReference = moduleReference;
        this.moduleReader = moduleReference.open();
        this.codeSource = new CodeSource(moduleReference.location().get().toURL(), (CodeSigner[]) null);
    }

    ModuleReference getModuleReference() {
        return moduleReference;
    }

    ModuleReader getModuleReader() {
        return moduleReader;
    }

    CodeSource getCodeSource() {
        return codeSource;
    }
}
