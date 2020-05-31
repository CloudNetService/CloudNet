package de.dytanic.cloudnet.driver.network.cluster;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public class NetworkClusterNodeExtensionSnapshot extends SerializableJsonDocPropertyable implements SerializableObject {

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

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getWebsite() {
        return this.website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void write(ProtocolBuffer buffer) {
        buffer.writeString(this.group);
        buffer.writeString(this.name);
        buffer.writeString(this.version);
        buffer.writeString(this.author);
        buffer.writeString(this.website);
        buffer.writeString(this.description);

        super.write(buffer);
    }

    @Override
    public void read(ProtocolBuffer buffer) {
        this.group = buffer.readString();
        this.name = buffer.readString();
        this.version = buffer.readString();
        this.author = buffer.readString();
        this.website = buffer.readString();
        this.description = buffer.readString();

        super.read(buffer);
    }
}