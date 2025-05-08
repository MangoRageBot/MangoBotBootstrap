package org.mangorage.bootstrap.internal;

import org.mangorage.bootstrap.api.IClassTransformer;

import java.io.IOException;
import java.lang.module.ModuleReader;
import java.lang.module.ResolvedModule;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public final class MangoLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private ClassTransformers transformers = new ClassTransformers(this);
    private final Map<String, LoadedModule> moduleMap = new HashMap<>();
    private final Map<String, LoadedModule> localPackageToModule = new HashMap<>();

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

    public void loadTransformers() {
        ServiceLoader.load(IClassTransformer.class)
                .stream()
                .forEach(provider -> {
                    transformers.add(provider.get());
                });
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (transformers == null || transformers.isEmpty())
            return super.findClass(name);

        if (transformers.containsClass(name))
            return transformers.getClazz(name);

        byte[] originalBytes = getClassBytes(name);

        if (originalBytes == null) {
            throw new ClassNotFoundException("Failed to load original class bytes for " + name);
        }

        byte[] arr = transformers.transform(name);
        if (arr != null) {
            Class<?> clz = defineClass(name, arr);
            transformers.add(name, clz);
            return clz;
        }

        return super.findClass(name);
    }

    private byte[] getClassBytes(String clazz) {
        try {
            String className = clazz.replace('.', '/');
            String classFileName = className + ".class";

            try (var is = getResourceAsStream(classFileName)) {
                if (is != null) return is.readAllBytes();
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private Class<?> defineClass(String name, byte[] bytes) {
        return super.defineClass(name, bytes, 0, bytes.length);
    }



    @Override
    protected URL findResource(String moduleName, String name) throws IOException {
        final var loadedModule = moduleMap.get(moduleName);

        if (loadedModule != null) {
            final var uri = loadedModule.getModuleReader().find(name);
            if (uri.isPresent())
                return uri.get().toURL();
        }

        return null;
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

            try {
                return defineClass(cn, bb, loadedModule.getCodeSource());
            } finally {
                reader.release(bb);
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
