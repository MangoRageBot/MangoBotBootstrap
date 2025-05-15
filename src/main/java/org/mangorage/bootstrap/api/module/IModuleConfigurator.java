package org.mangorage.bootstrap.api.module;

import java.util.List;

public interface IModuleConfigurator {
    /**
     * Only used for fetching Resources
     *
     * <p>
     * Allows the Classloader to fetch
     * resources from the module's children as well
     * <p>
     * We DON'T do this for getting classes...
     * <p>
     * Gets the children modules for this module
     * @return All the children nodules...
     */
    default List<String> getChildren(String module) {
        return List.of();
    }

    /**
     * Allows for further configuration of the module layer...
     * <p>
     * Such as adding additional opens/exports
     */
    default void configureModuleLayer(IModuleLayer moduleLayer) {

    }
}
