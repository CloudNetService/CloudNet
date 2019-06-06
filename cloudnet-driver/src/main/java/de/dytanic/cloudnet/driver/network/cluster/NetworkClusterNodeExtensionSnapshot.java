package de.dytanic.cloudnet.driver.network.cluster;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
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

}