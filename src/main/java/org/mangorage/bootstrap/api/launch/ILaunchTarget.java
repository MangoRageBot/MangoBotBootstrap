package org.mangorage.bootstrap.api.launch;

public interface ILaunchTarget {
    String getId();

    void launch(ModuleLayer parent, String[] args) throws Throwable;
}
