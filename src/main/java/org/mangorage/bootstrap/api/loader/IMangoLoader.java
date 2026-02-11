package org.mangorage.bootstrap.api.loader;


import org.mangorage.bootstrap.api.transformer.IClassTransformerHistory;

public interface IMangoLoader {

    IClassTransformerHistory getTransformerHistory();

    byte[] getClassBytes(String name);
    boolean hasClass(String name);
}
