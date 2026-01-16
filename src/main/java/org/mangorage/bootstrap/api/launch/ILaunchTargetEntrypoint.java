package org.mangorage.bootstrap.api.launch;

/**
 * Entrypoint for launching
 */
public interface ILaunchTargetEntrypoint {
    /**
     * @return should match the same id of a launch target {@link ILaunchTarget}).
     */
    String getLaunchTargetId();
    void init(String[] args);
}
