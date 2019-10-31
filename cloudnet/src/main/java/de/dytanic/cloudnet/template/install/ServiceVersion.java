package de.dytanic.cloudnet.template.install;

import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.util.Optional;

public class ServiceVersion {
    private String name;
    private String url;
    private int minJavaVersion, maxJavaVersion;
    private JsonDocument properties;

    public ServiceVersion(String name, String url) {
        this(name, url, new JsonDocument());
    }

    public ServiceVersion(String name, String url, JsonDocument properties) {
        this(name, url, 0, 0, properties);
    }

    public ServiceVersion(String name, String url, JavaVersion minJavaVersion, JavaVersion maxJavaVersion) {
        this(name, url, minJavaVersion, maxJavaVersion, new JsonDocument());
    }

    public ServiceVersion(String name, String url, JavaVersion minJavaVersion, JavaVersion maxJavaVersion, JsonDocument properties) {
        this(name, url, minJavaVersion != null ? minJavaVersion.getVersion() : 0, maxJavaVersion != null ? maxJavaVersion.getVersion() : 0, properties);
    }

    public ServiceVersion(String name, String url, int minJavaVersion, int maxJavaVersion) {
        this(name, url, minJavaVersion, maxJavaVersion, new JsonDocument());
    }

    public ServiceVersion(String name, String url, int minJavaVersion, int maxJavaVersion, JsonDocument properties) {
        this.name = name;
        this.url = url;
        this.minJavaVersion = minJavaVersion;
        this.maxJavaVersion = maxJavaVersion;
        this.properties = properties;
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

    public boolean canRunOn(JavaVersion javaVersion) {
        Optional<JavaVersion> minJavaVersion = JavaVersion.fromVersion(this.minJavaVersion);
        Optional<JavaVersion> maxJavaVersion = JavaVersion.fromVersion(this.maxJavaVersion);
        if (minJavaVersion.isPresent() && maxJavaVersion.isPresent()) {
            return javaVersion.isSupported(minJavaVersion.get(), maxJavaVersion.get());
        }
        return minJavaVersion.map(javaVersion::isMinimalVersion)
                .orElseGet(() -> maxJavaVersion
                        .map(javaVersion::isMaximalVersion)
                        .orElse(true)
                );
    }

}
