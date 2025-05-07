package org.mangorage.bootstrap.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.jar.Manifest;

public final class MangoLoader extends URLClassLoader {
    public MangoLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected URL findResource(String moduleName, String name) throws IOException {
        final var first = findResource(name);
        if (first == null) {
            if (getParent() instanceof URLClassLoader classLoader)
                return classLoader.findResource(name);
        }

        return findResource(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    public URL findResource(String name) {
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        return super.findResources(name);
    }


    @Override
    protected Package definePackage(String name, Manifest man, URL url) {
        return super.definePackage(name, man, url);
    }

    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        return super.getPermissions(codesource);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return super.getResourceAsStream(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String moduleName, String name) {
        final var clz = super.findClass(moduleName, name);
        if (clz == null) {
            try {
                return super.findClass(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public URL getResource(String name) {
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return super.getResources(name);
    }

    @Override
    protected String findLibrary(String libname) {
        return super.findLibrary(libname);
    }
}
