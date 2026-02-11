package org.mangorage.bootstrap.api.transformer;

import java.util.List;

public interface IClassTransformerHistory {
    List<ITransformerResultHistory> getHistory(String name);

    default List<ITransformerResultHistory> getHistory(Class<?> clazz) {
        return getHistory(clazz.getName());
    }
}
