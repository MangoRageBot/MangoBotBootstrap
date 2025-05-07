package org.mangorage.bootstrap.internal;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public final class MangoLoader extends URLClassLoader {
    public MangoLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected URL findResource(String moduleName, String name) throws IOException {
        return super.findResource(name);
    }
}
