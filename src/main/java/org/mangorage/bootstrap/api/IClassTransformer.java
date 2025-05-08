package org.mangorage.bootstrap.api;

public interface IClassTransformer {
    TransformResult transform(String className, byte[] classData);

    String getName();
}