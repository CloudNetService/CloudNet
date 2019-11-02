package de.dytanic.cloudnet.template.install;

import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.util.Optional;

public class ServiceVersion {
    private String name;
    private String url;
    private int minJavaVersion, maxJavaVersion;
    private JsonDocument properties = new JsonDocument();

    public ServiceVersion(String name, String url, int minJavaVersion, int maxJavaVersion, JsonDocument properties) {
        this.name = name;
        this.url = url;
        this.minJavaVersion = minJavaVersion;
        this.maxJavaVersion = maxJavaVersion;
        this.properties = properties;
    }

    public ServiceVersion() {
    }

    public boolean isLatest() {
        return this.name.equalsIgnoreCase("latest");
    }

    public boolean canRun() {
        return this.canRun(JavaVersion.getRuntimeVersion());
    }

    public boolean canRun(JavaVersion javaVersion) {
        Optional<JavaVersion> minJavaVersion = JavaVersion.fromVersion(this.minJavaVersion);
        Optional<JavaVersion> maxJavaVersion = JavaVersion.fromVersion(this.maxJavaVersion);

        if (minJavaVersion.isPresent() && maxJavaVersion.isPresent()) {
            return javaVersion.isSupported(minJavaVersion.get(), maxJavaVersion.get());
        }

        return minJavaVersion.map(javaVersion::isSupportedByMin)
                .orElseGet(() -> maxJavaVersion
                        .map(javaVersion::isSupportedByMax)
                        .orElse(true)
                );
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public JsonDocument getProperties() {
        return properties;
    }


}
