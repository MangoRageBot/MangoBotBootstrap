package org.mangorage.bootstrap.api.loader;


public interface IMangoLoader {
    byte[] getClassBytes(String name);
    boolean hasClass(String name);
}
