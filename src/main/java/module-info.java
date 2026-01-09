module org.mangorage.bootstrap {
    requires java.scripting;
    requires static com.google.gson;
    requires static java.sql;

    exports org.mangorage.bootstrap.api.transformer;
    exports org.mangorage.bootstrap.api.module;
    exports org.mangorage.bootstrap.api.launch;
    exports org.mangorage.bootstrap.api.loader;

    opens org.mangorage.bootstrap;

    uses org.mangorage.bootstrap.api.transformer.IClassTransformer;
    uses org.mangorage.bootstrap.api.module.IModuleConfigurator;
    uses org.mangorage.bootstrap.api.launch.ILaunchTarget;
}