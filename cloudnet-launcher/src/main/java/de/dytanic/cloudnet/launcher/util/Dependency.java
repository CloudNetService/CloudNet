package de.dytanic.cloudnet.launcher.util;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Dependency)) return false;
        final Dependency other = (Dependency) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$repository = this.getRepository();
        final Object other$repository = other.getRepository();
        if (this$repository == null ? other$repository != null : !this$repository.equals(other$repository))
            return false;
        final Object this$group = this.getGroup();
        final Object other$group = other.getGroup();
        if (this$group == null ? other$group != null : !this$group.equals(other$group)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$version = this.getVersion();
        final Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final Object this$classifier = this.getClassifier();
        final Object other$classifier = other.getClassifier();
        if (this$classifier == null ? other$classifier != null : !this$classifier.equals(other$classifier))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Dependency;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $repository = this.getRepository();
        result = result * PRIME + ($repository == null ? 43 : $repository.hashCode());
        final Object $group = this.getGroup();
        result = result * PRIME + ($group == null ? 43 : $group.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final Object $classifier = this.getClassifier();
        result = result * PRIME + ($classifier == null ? 43 : $classifier.hashCode());
        return result;
    }
}