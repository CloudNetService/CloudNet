package de.dytanic.cloudnet.driver.service;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

@ToString
@EqualsAndHashCode
public final class ServiceId {

    private final UUID uniqueId;

    private final String nodeUniqueId, taskName;

    private final int taskServiceId;

    private final ServiceEnvironmentType environment;

    public ServiceId(UUID uniqueId, String nodeUniqueId, String taskName, int taskServiceId, ServiceEnvironmentType environment) {
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