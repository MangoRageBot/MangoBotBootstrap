module org.mangorage.bootstrap {
    requires java.scripting;
    requires static com.google.gson;
    requires static java.sql;

    exports org.mangorage.bootstrap.api.dependency;
    exports org.mangorage.bootstrap.api.launch;
    exports org.mangorage.bootstrap.api.lifecycle;
    exports org.mangorage.bootstrap.api.loader;
    exports org.mangorage.bootstrap.api.logging;
    exports org.mangorage.bootstrap.api.module;
    exports org.mangorage.bootstrap.api.transformer;
    exports org.mangorage.bootstrap.api.util;

    opens org.mangorage.bootstrap;

    uses org.mangorage.bootstrap.api.launch.ILaunchTarget;
    uses org.mangorage.bootstrap.api.lifecycle.IBootstrapLifecycle;
    uses org.mangorage.bootstrap.api.logging.ILoggerProvider;
}