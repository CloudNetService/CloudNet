package de.dytanic.cloudnet.driver.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

@EqualsAndHashCode
/**
 * Defines the location of a template for services that can either be copied into a service or filled from a service
 * by using a {@link ServiceDeployment}. CloudNet's default storage is "local".
 */
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

    /**
     * Parses a template out of a string in the following format: storage:prefix/name
     * "storage:" is optional, only "prefix/name" needs to be provided
     * <p>
     * {@code alwaysCopyToStaticServices} will always be false in the returned {@link ServiceTemplate}.
     *
     * @param template the template in the specified format
     * @return the parsed {@link ServiceTemplate} or null if the format was invalid
     */
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

    /**
     * Parses multiple templates out of a string in the format specified for {@link #parse(String)} split by ";".
     *
     * @param templates the templates in the specified format
     * @return an array of the parsed templates, this will not contain any null elements if any format is wrong
     */
    @NotNull
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
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeString(this.prefix);
        buffer.writeString(this.name);
        buffer.writeString(this.storage);
        buffer.writeBoolean(this.alwaysCopyToStaticServices);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.prefix = buffer.readString();
        this.name = buffer.readString();
        this.storage = buffer.readString();
        this.alwaysCopyToStaticServices = buffer.readBoolean();
    }
}