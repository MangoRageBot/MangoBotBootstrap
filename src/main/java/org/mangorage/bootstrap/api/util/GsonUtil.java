package org.mangorage.bootstrap.api.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class GsonUtil {
    private static final Gson GSON = new GsonBuilder().create();

    public static <T> T get(Class<T> tClass, String json) {
        return GSON.fromJson(json, tClass);
    }
}
