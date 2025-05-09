package org.mangorage.bootstrap.api.transformer;

public enum TransformerFlag {
    NO_REWRITE,
    SIMPLE_REWRITE,
    FULL_REWRITE;

    public TransformResult of(byte[] classData) {
        return new TransformResult(classData, this);
    }
}
