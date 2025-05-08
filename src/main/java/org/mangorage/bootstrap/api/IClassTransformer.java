package org.mangorage.bootstrap.api;

public interface IClassTransformer {
    TransformResult transform(byte[] classData);

    String getName();
}