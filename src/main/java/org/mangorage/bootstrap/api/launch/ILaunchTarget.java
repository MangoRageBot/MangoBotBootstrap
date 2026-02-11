package org.mangorage.bootstrap.api.launch;

public interface ILaunchTarget {
    String getId();

    /**
     * @return The ModuleLayer that was created!
     */
    ModuleLayer launch(ModuleLayer bootstrapLayer, ModuleLayer parent, String[] args) throws Throwable;
}
