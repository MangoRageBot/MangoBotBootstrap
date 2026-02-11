package org.mangorage.bootstrap.api.transformer;

public interface ITransformerResultHistory {
    byte[] classData();
    ITransformerResultHistory previous();

    Class<?> transformer();
    String transformerName();
    TransformerFlag transformerFlag();
    byte[] transformerResult();
}
