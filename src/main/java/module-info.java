module org.mangorage.bootstrap {
    requires jdk.unsupported;
    requires java.scripting;

    opens org.mangorage.bootstrap;
    exports org.mangorage.bootstrap.api.transformer;
    exports org.mangorage.bootstrap.api.module;
    exports org.mangorage.bootstrap.api.launch;

    uses org.mangorage.bootstrap.api.transformer.IClassTransformer;
    uses org.mangorage.bootstrap.api.module.IModuleConfigurator;
    uses org.mangorage.bootstrap.api.launch.ILaunchTarget;
}