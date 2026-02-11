package org.mangorage.bootstrap.api.transformer;

public interface ITransformerResultHistory {
    Class<?> transformer();
    TransformerFlag transformerFlag();
    byte[] classData();
}
