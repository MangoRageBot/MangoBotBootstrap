package org.mangorage.bootstrap.api.launch;

public interface ILaunchTarget {
    String getId();

    /**
     * Set up the module layer and everything!
     *
     * @return The ModuleLayer that was created!
     */
    ModuleLayer setup(ModuleLayer bootstrapLayer, ModuleLayer parent, String[] args) throws Throwable;

    void launch(ModuleLayer moduleLayer, String[] args);
}
