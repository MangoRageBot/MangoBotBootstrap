package org.mangorage.bootstrap.api.transformer;

public interface ITransformerResultHistory {
    Class<?> transformer();
    String transformerName();
    TransformerFlag transformerFlag();
    byte[] classData();
}
