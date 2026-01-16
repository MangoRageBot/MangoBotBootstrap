module org.mangorage.bootstrap {
    requires java.scripting;
    requires static com.google.gson;
    requires static java.sql;

    exports org.mangorage.bootstrap.api.transformer;
    exports org.mangorage.bootstrap.api.module;
    exports org.mangorage.bootstrap.api.launch;
    exports org.mangorage.bootstrap.api.loader;

    opens org.mangorage.bootstrap;

    provides org.mangorage.bootstrap.api.launch.ILaunchTarget with org.mangorage.bootstrap.internal.launch.MangoBotLaunchTarget;
    provides org.mangorage.bootstrap.api.dependency.IDependencyLocator with org.mangorage.bootstrap.internal.launch.MangoBotDependencyLocator;

    uses org.mangorage.bootstrap.api.transformer.IClassTransformer;
    uses org.mangorage.bootstrap.api.module.IModuleConfigurator;
    uses org.mangorage.bootstrap.api.launch.ILaunchTarget;
    uses org.mangorage.bootstrap.api.dependency.IDependencyLocator;
    uses org.mangorage.bootstrap.api.launch.ILaunchTargetEntrypoint;
}