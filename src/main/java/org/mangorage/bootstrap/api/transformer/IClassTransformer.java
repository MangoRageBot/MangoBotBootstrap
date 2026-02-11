package org.mangorage.bootstrap.api.transformer;

public interface IClassTransformer {
    TransformResult transform(String className, byte[] classData);
    String getName();
}