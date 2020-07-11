package de.dytanic.cloudnet.driver.module.dependency;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class ModuleRepository implements SerializableObject {

    private String name, url;

    public ModuleRepository(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public ModuleRepository() {
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeString(this.name);
        buffer.writeString(this.url);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.name = buffer.readString();
        this.url = buffer.readString();
    }
}