package org.mangorage.bootstrap.api.loader;

import org.mangorage.bootstrap.internal.MangoLoaderImpl;

public sealed interface IMangoLoader permits MangoLoaderImpl {
    byte[] getClassBytes(String name);
    boolean hasClass(String name);
}
