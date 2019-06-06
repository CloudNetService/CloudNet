package de.dytanic.cloudnet.driver.module;

public class ModuleDependency {

    private String repo, url, group, name, version;

    public ModuleDependency(String repo, String url, String group, String name, String version) {
        this.repo = repo;
        this.url = url;
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public ModuleDependency() {
    }

    public String getRepo() {
        return this.repo;
    }

    public String getUrl() {
        return this.url;
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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ModuleDependency)) return false;
        final ModuleDependency other = (ModuleDependency) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$repo = this.getRepo();
        final Object other$repo = other.getRepo();
        if (this$repo == null ? other$repo != null : !this$repo.equals(other$repo)) return false;
        final Object this$url = this.getUrl();
        final Object other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) return false;
        final Object this$group = this.getGroup();
        final Object other$group = other.getGroup();
        if (this$group == null ? other$group != null : !this$group.equals(other$group)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$version = this.getVersion();
        final Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ModuleDependency;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $repo = this.getRepo();
        result = result * PRIME + ($repo == null ? 43 : $repo.hashCode());
        final Object $url = this.getUrl();
        result = result * PRIME + ($url == null ? 43 : $url.hashCode());
        final Object $group = this.getGroup();
        result = result * PRIME + ($group == null ? 43 : $group.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        return result;
    }

    public String toString() {
        return "ModuleDependency(repo=" + this.getRepo() + ", url=" + this.getUrl() + ", group=" + this.getGroup() + ", name=" + this.getName() + ", version=" + this.getVersion() + ")";
    }
}