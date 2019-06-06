package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

public class ModuleConfiguration {

    protected boolean runtimeModule;

    protected String
            group,
            name,
            version,
            main,
            description,
            author,
            website;

    //protected ModuleUpdateServiceConfiguration updateServiceConfiguration;

    protected ModuleRepository[] repos;

    protected ModuleDependency[] dependencies;

    protected JsonDocument properties;

    public ModuleConfiguration(boolean runtimeModule, String group, String name, String version, String main, String description, String author, String website, ModuleRepository[] repos, ModuleDependency[] dependencies, JsonDocument properties) {
        this.runtimeModule = runtimeModule;
        this.group = group;
        this.name = name;
        this.version = version;
        this.main = main;
        this.description = description;
        this.author = author;
        this.website = website;
        this.repos = repos;
        this.dependencies = dependencies;
        this.properties = properties;
    }

    public ModuleConfiguration() {
    }

    public String getMainClass() {
        return this.main;
    }

    public boolean isRuntimeModule() {
        return this.runtimeModule;
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

    public String getMain() {
        return this.main;
    }

    public String getDescription() {
        return this.description;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getWebsite() {
        return this.website;
    }

    public ModuleRepository[] getRepos() {
        return this.repos;
    }

    public ModuleDependency[] getDependencies() {
        return this.dependencies;
    }

    public JsonDocument getProperties() {
        return this.properties;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ModuleConfiguration)) return false;
        final ModuleConfiguration other = (ModuleConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isRuntimeModule() != other.isRuntimeModule()) return false;
        final Object this$group = this.getGroup();
        final Object other$group = other.getGroup();
        if (this$group == null ? other$group != null : !this$group.equals(other$group)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$version = this.getVersion();
        final Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final Object this$main = this.getMain();
        final Object other$main = other.getMain();
        if (this$main == null ? other$main != null : !this$main.equals(other$main)) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description))
            return false;
        final Object this$author = this.getAuthor();
        final Object other$author = other.getAuthor();
        if (this$author == null ? other$author != null : !this$author.equals(other$author)) return false;
        final Object this$website = this.getWebsite();
        final Object other$website = other.getWebsite();
        if (this$website == null ? other$website != null : !this$website.equals(other$website)) return false;
        if (!java.util.Arrays.deepEquals(this.getRepos(), other.getRepos())) return false;
        if (!java.util.Arrays.deepEquals(this.getDependencies(), other.getDependencies())) return false;
        final Object this$properties = this.getProperties();
        final Object other$properties = other.getProperties();
        if (this$properties == null ? other$properties != null : !this$properties.equals(other$properties))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ModuleConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isRuntimeModule() ? 79 : 97);
        final Object $group = this.getGroup();
        result = result * PRIME + ($group == null ? 43 : $group.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final Object $main = this.getMain();
        result = result * PRIME + ($main == null ? 43 : $main.hashCode());
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $author = this.getAuthor();
        result = result * PRIME + ($author == null ? 43 : $author.hashCode());
        final Object $website = this.getWebsite();
        result = result * PRIME + ($website == null ? 43 : $website.hashCode());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getRepos());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getDependencies());
        final Object $properties = this.getProperties();
        result = result * PRIME + ($properties == null ? 43 : $properties.hashCode());
        return result;
    }

    public String toString() {
        return "ModuleConfiguration(runtimeModule=" + this.isRuntimeModule() + ", group=" + this.getGroup() + ", name=" + this.getName() + ", version=" + this.getVersion() + ", main=" + this.getMain() + ", description=" + this.getDescription() + ", author=" + this.getAuthor() + ", website=" + this.getWebsite() + ", repos=" + java.util.Arrays.deepToString(this.getRepos()) + ", dependencies=" + java.util.Arrays.deepToString(this.getDependencies()) + ", properties=" + this.getProperties() + ")";
    }
}