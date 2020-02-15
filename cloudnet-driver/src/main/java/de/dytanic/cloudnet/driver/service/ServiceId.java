package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@ToString
@EqualsAndHashCode
public final class ServiceId implements INameable {

    private final UUID uniqueId;

    private final String nodeUniqueId, taskName;

    private final int taskServiceId;

    private final ServiceEnvironmentType environment;

    public ServiceId(@NotNull UUID uniqueId, @NotNull String nodeUniqueId, String taskName, int taskServiceId, ServiceEnvironmentType environment) {
        this.uniqueId = uniqueId;
        this.nodeUniqueId = nodeUniqueId;
        this.taskName = taskName;
        this.taskServiceId = taskServiceId;
        this.environment = environment;
    }

    public String getName() {
        return taskName + "-" + taskServiceId;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getNodeUniqueId() {
        return this.nodeUniqueId;
    }

    public String getTaskName() {
        return this.taskName;
    }

    public int getTaskServiceId() {
        return this.taskServiceId;
    }

    public ServiceEnvironmentType getEnvironment() {
        return this.environment;
    }

}