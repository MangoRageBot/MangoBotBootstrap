package org.mangorage.bootstrap.internal;

import org.mangorage.bootstrap.api.module.IModuleConfigurator;
import org.mangorage.bootstrap.api.transformer.IClassTransformer;

import java.io.IOException;
import java.lang.module.ModuleReader;
import java.lang.module.ResolvedModule;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MangoLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private ClassTransformers transformers = new ClassTransformers(this);
    private final Map<String, LoadedModule> moduleMap = new ConcurrentHashMap<>();
    private final Map<String, LoadedModule> localPackageToModule = new ConcurrentHashMap<>();



    public MangoLoader(URL[] urls, Set<ResolvedModule> modules, ClassLoader parent) {
        super(urls, parent);

        modules.forEach(module -> {
            try {
                final var loadedModule = new LoadedModule(module.reference());

                moduleMap.put(
                        module.name(),
                        loadedModule
                );

                module.reference().descriptor().packages().forEach(pkg -> {
                    localPackageToModule.put(pkg, loadedModule);
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void load() {
        loadTransformers();
        loadModuleConfiguration();
    }

    void loadTransformers() {
        ServiceLoader.load(IClassTransformer.class)
                .stream()
                .forEach(provider -> {
                    transformers.add(provider.get());
                });
    }

    void loadModuleConfiguration() {
        ServiceLoader.load(IModuleConfigurator.class)
                .stream()
                .forEach(provider -> {
                    final var configurator = provider.get();

                    moduleMap.forEach((id, module) -> {
                        final var children = configurator.getChildren(id);

                        children
                                .stream()
                                .filter(moduleMap::containsKey)
                                .map(moduleMap::get)
                                .forEach(module::addChild);
                    });
                });
    }

//    /**
//     * Loads the class with the specified binary name.
//     */
//    @Override
//    protected Class<?> loadClass(String cn, boolean resolve) throws ClassNotFoundException
//    {
//
//        synchronized (getClassLoadingLock(cn)) {
//            // check if already loaded
//            Class<?> c = findLoadedClass(cn);
//
//            if (c == null) {
//
//                LoadedModule loadedModule = findLoadedModule(cn);
//
//                if (loadedModule != null) {
//
//                    // class is in module defined to this class loader
//                    c = defineClass(cn, loadedModule);
//
//                } else {
//                    return getParent().loadClass(cn);
//                }
//            }
//
//            if (c == null)
//                throw new ClassNotFoundException(cn);
//
//            if (resolve)
//                resolveClass(c);
//
//            return c;
//        }
//    }

    @Override
    protected URL findResource(String moduleName, String name) throws IOException {
        final var loadedModule = moduleMap.get(moduleName);

        if (loadedModule != null) {
            final var uri = loadedModule.find(name);
            if (uri.isPresent())
                return uri.get().toURL();
        }

        return null;
    }

    @Override
    protected Class<?> findClass(String cn) throws ClassNotFoundException {
        Class<?> c = null;
        LoadedModule loadedModule = findLoadedModule(cn);
        if (loadedModule != null)
            c = defineClass(cn, loadedModule);
        if (c == null)
            throw new ClassNotFoundException(cn);
        return c;
    }

    @Override
    protected Class<?> findClass(String moduleName, String name) {
        Class<?> c = null;
        LoadedModule loadedModule = findLoadedModule(name);
        if (loadedModule != null && loadedModule.getModuleReference().descriptor().name().equals(moduleName)) {
            c = defineClass(name, loadedModule);
        } else if (loadedModule != null) {
            throw new IllegalArgumentException("Expected Class '%s' in module '%s', instead was in '%s'".formatted(name, moduleName, loadedModule.getModuleReference().descriptor().name()));
        }
        return c;
    }

    /**
     * Defines the given binary class name to the VM, loading the class
     * bytes from the given module.
     *
     * @return the resulting Class or {@code null} if an I/O error occurs
     */
    private Class<?> defineClass(String cn, LoadedModule loadedModule) {
        ModuleReader reader = loadedModule.getModuleReader();

        try {
            // read class file
            String rn = cn.replace('.', '/').concat(".class");
            ByteBuffer bb = reader.read(rn).orElse(null);
            if (bb == null) {
                // class not found
                return null;
            }

            if (transformers.containsClass(cn))
                return transformers.getClazz(cn);

            byte[] classbytes = bb.array();

            byte[] classBytesModified = transformers.transform(cn, classbytes);

            if (classBytesModified != null) {
                Class<?> clz = defineClass(cn, classBytesModified, 0, classBytesModified.length, loadedModule.getCodeSource());
                transformers.add(cn, clz);
                return clz;
            } else {
                try {
                    return defineClass(cn, bb, loadedModule.getCodeSource());
                } finally {
                    reader.release(bb);
                }
            }
        } catch (IOException ioe) {
            // TBD on how I/O errors should be propagated
            return null;
        }
    }

    /**
     * Find the candidate module for the given class name.
     * Returns {@code null} if none of the modules defined to this
     * class loader contain the API package for the class.
     */
    private LoadedModule findLoadedModule(String cn) {
        String pn = packageName(cn);
        return pn.isEmpty() ? null : localPackageToModule.get(pn);
    }

    /**
     * Returns the package name for the given class name
     */
    private String packageName(String cn) {
        int pos = cn.lastIndexOf('.');
        return (pos < 0) ? "" : cn.substring(0, pos);
    }
}
