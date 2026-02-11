package org.mangorage.bootstrap.api.lifecycle;

public interface IBootstrapLifecycle {
    void onError(Throwable throwable, ModuleLayer moduleLayer);
}
