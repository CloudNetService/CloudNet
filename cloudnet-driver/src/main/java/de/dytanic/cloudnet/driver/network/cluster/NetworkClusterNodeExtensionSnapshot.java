package de.dytanic.cloudnet.driver.network.cluster;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;

public class NetworkClusterNodeExtensionSnapshot extends BasicJsonDocPropertyable {

    protected String group, name, version, author, website, description;

    public NetworkClusterNodeExtensionSnapshot(String group, String name, String version, String author, String website, String description) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.author = author;
        this.website = website;
        this.description = description;
    }

    public NetworkClusterNodeExtensionSnapshot() {
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

    public String getAuthor() {
        return this.author;
    }

    public String getWebsite() {
        return this.website;
    }

    public String getDescription() {
        return this.description;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String toString() {
        return "NetworkClusterNodeExtensionSnapshot(group=" + this.getGroup() + ", name=" + this.getName() + ", version=" + this.getVersion() + ", author=" + this.getAuthor() + ", website=" + this.getWebsite() + ", description=" + this.getDescription() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NetworkClusterNodeExtensionSnapshot))
            return false;
        final NetworkClusterNodeExtensionSnapshot other = (NetworkClusterNodeExtensionSnapshot) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$group = this.getGroup();
        final Object other$group = other.getGroup();
        if (this$group == null ? other$group != null : !this$group.equals(other$group)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$version = this.getVersion();
        final Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final Object this$author = this.getAuthor();
        final Object other$author = other.getAuthor();
        if (this$author == null ? other$author != null : !this$author.equals(other$author)) return false;
        final Object this$website = this.getWebsite();
        final Object other$website = other.getWebsite();
        if (this$website == null ? other$website != null : !this$website.equals(other$website)) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof NetworkClusterNodeExtensionSnapshot;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $group = this.getGroup();
        result = result * PRIME + ($group == null ? 43 : $group.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final Object $author = this.getAuthor();
        result = result * PRIME + ($author == null ? 43 : $author.hashCode());
        final Object $website = this.getWebsite();
        result = result * PRIME + ($website == null ? 43 : $website.hashCode());
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        return result;
    }
}