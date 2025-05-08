package org.mangorage.bootstrap.internal;

import org.mangorage.bootstrap.api.IClassTransformer;
import org.mangorage.bootstrap.api.TransformResult;
import org.mangorage.bootstrap.api.TransformerFlag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ClassTransformers {
    private final Map<String, Class<?>> classes = new HashMap<>(); // Transformed Class's
    private final List<IClassTransformer> transformers = new ArrayList<>(); // Transformer's
    private final ClassLoader loader;

    public ClassTransformers(ClassLoader loader) {
        this.loader = loader;
    }

    public void add(String name, Class<?> clz) {
        classes.put(name, clz);
    }

    public void add(IClassTransformer transformer) {
        transformers.add(transformer);
    }

    public boolean isEmpty() {
        return transformers.isEmpty();
    }

    private byte[] getClassBytes(String clazz) {
        try {
            String className = clazz.replace('.', '/');
            String classFileName = className + ".class";

            try (var is = loader.getResourceAsStream(classFileName)) {
                if (is != null) return is.readAllBytes();
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public byte[] transform(String name) {
        byte[] originalClassData = getClassBytes(name);

        AtomicReference<TransformResult> result = new AtomicReference<>(TransformerFlag.NO_REWRITE.of(originalClassData));
        AtomicReference<IClassTransformer> _transformer = new AtomicReference<>();

        for (IClassTransformer transformer : transformers) {
            result.set(transformer.transform(originalClassData));
            if (result.get().flag() != TransformerFlag.NO_REWRITE) {
                _transformer.set(transformer);
                break;
            }
        }

        if (result.get().flag() != TransformerFlag.NO_REWRITE && _transformer.get() != null) {
            System.out.println("%s Transformed %s".formatted(_transformer.get().getName(), name));
            return result.get().classData();
        }

        return null;
    }

    public boolean containsClass(String name) {
        return classes.containsKey(name);
    }

    public Class<?> getClazz(String string) {
        return classes.get(string);
    }
}