package de.dytanic.cloudnet.launcher.version;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Dependency {

    private final String repository, group, name, version;

    private String classifier;

    public Dependency(String repository, String group, String name, String version) {
        this.repository = repository;
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public Dependency(String repository, String group, String name, String version, String classifier) {
        this.repository = repository;
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
    }

    public String getRepository() {
        return this.repository;
    }

    public String getGroup() {
        return this.group;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getClassifier() {
        return this.classifier;
    }

}