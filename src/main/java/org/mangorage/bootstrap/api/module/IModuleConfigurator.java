package org.mangorage.bootstrap.api.module;

import java.util.List;

public interface IModuleConfigurator {
    List<String> getChildren(String module);
}
