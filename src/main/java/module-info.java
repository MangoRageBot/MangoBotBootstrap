module org.mangorage.bootstrap {
    requires jdk.unsupported;
    requires java.scripting;

    opens org.mangorage.bootstrap;
    exports org.mangorage.bootstrap.api.transformer;
    exports org.mangorage.bootstrap.api.module;

    uses org.mangorage.bootstrap.api.transformer.IClassTransformer;
    uses org.mangorage.bootstrap.api.module.IModuleConfigurator;
}