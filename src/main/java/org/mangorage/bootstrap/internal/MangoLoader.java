package org.mangorage.bootstrap.internal;


import java.io.IOException;
import java.lang.module.ModuleReader;
import java.lang.module.ResolvedModule;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MangoLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

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
        if (loadedModule != null && loadedModule.getModuleReference().descriptor().name().equals(moduleName))
            c = defineClass(name, loadedModule);
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
