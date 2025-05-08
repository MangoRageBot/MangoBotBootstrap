module org.mangorage.bootstrap {
    requires jdk.unsupported;
    requires java.scripting;

    opens org.mangorage.bootstrap;
    exports org.mangorage.bootstrap.api;

    uses org.mangorage.bootstrap.api.IClassTransformer;
}