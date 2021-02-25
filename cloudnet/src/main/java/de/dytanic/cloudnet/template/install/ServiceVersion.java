package de.dytanic.cloudnet.template.install;

import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServiceVersion {
    private final Map<String, String> additionalDownloads = new HashMap<>();
    private String name;
    private String url;
    private int minJavaVersion;
    private int maxJavaVersion;
    private boolean deprecated;
    private boolean cacheFiles = true;
    private JsonDocument properties = new JsonDocument();

    public ServiceVersion(String name, String url, int minJavaVersion, int maxJavaVersion, boolean deprecated, boolean cacheFiles, JsonDocument properties) {
        this.name = name;
        this.url = url;
        this.minJavaVersion = minJavaVersion;
        this.maxJavaVersion = maxJavaVersion;
        this.deprecated = deprecated;
        this.cacheFiles = cacheFiles;
        this.properties = properties;
    }

    public ServiceVersion() {
    }

    public boolean isCacheFiles() {
        return this.cacheFiles;
    }

    public boolean isDeprecated() {
        return this.deprecated;
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
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JsonDocument getProperties() {
        return this.properties;
    }

    public Map<String, String> getAdditionalDownloads() {
        return this.additionalDownloads;
    }
}
