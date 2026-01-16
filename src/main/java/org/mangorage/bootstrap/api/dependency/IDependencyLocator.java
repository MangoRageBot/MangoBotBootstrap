package org.mangorage.bootstrap.api.dependency;

import java.util.List;

public interface IDependencyLocator {
    boolean isValidLocatorFor(String launchTarget);
    List<IDependency> locate();
}
