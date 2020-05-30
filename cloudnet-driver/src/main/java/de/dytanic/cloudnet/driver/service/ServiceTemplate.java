package de.dytanic.cloudnet.driver.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Collection;

@EqualsAndHashCode
public class ServiceTemplate implements INameable, SerializableObject {

    private String prefix, name, storage;
    private boolean alwaysCopyToStaticServices;

    public ServiceTemplate(String prefix, String name, String storage) {
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(storage);

        this.prefix = prefix;
        this.name = name;
        this.storage = storage;
    }

    public ServiceTemplate(String prefix, String name, String storage, boolean alwaysCopyToStaticServices) {
        this(prefix, name, storage);
        this.alwaysCopyToStaticServices = alwaysCopyToStaticServices;
    }

    public ServiceTemplate() {
    }

    public boolean shouldAlwaysCopyToStaticServices() {
        return this.alwaysCopyToStaticServices;
    }

    @Override
    public String toString() {
        return this.storage + ":" + this.prefix + "/" + this.name;
    }

    public String getTemplatePath() {
        return this.prefix + "/" + this.name;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getName() {
        return this.name;
    }

    public String getStorage() {
        return this.storage;
    }

    public static ServiceTemplate parse(String template) {
        String[] base = template.split(":");

        if (base.length > 2) {
            return null;
        }

        String path = base.length == 2 ? base[1] : base[0];
        String storage = base.length == 2 ? base[0] : "local";
        String[] splitPath = path.split("/");

        if (splitPath.length != 2) {
            return null;
        }

        return new ServiceTemplate(splitPath[0], splitPath[1], storage);
    }

    public static ServiceTemplate[] parseArray(String templates) {
        Collection<ServiceTemplate> result = new ArrayList<>();
        for (String template : templates.split(";")) {
            ServiceTemplate serviceTemplate = parse(template);
            if (serviceTemplate != null) {
                result.add(serviceTemplate);
            }
        }
        return result.toArray(new ServiceTemplate[0]);
    }

    @Override
    public void write(ProtocolBuffer buffer) {
        buffer.writeString(this.prefix);
        buffer.writeString(this.name);
        buffer.writeString(this.storage);
        buffer.writeBoolean(this.alwaysCopyToStaticServices);
    }

    @Override
    public void read(ProtocolBuffer buffer) {
        this.prefix = buffer.readString();
        this.name = buffer.readString();
        this.storage = buffer.readString();
        this.alwaysCopyToStaticServices = buffer.readBoolean();
    }
}