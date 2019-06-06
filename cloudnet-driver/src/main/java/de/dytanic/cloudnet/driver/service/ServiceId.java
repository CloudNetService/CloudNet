package de.dytanic.cloudnet.driver.service;

import java.util.UUID;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ServiceId)) return false;
        final ServiceId other = (ServiceId) o;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        final Object this$nodeUniqueId = this.getNodeUniqueId();
        final Object other$nodeUniqueId = other.getNodeUniqueId();
        if (this$nodeUniqueId == null ? other$nodeUniqueId != null : !this$nodeUniqueId.equals(other$nodeUniqueId))
            return false;
        final Object this$taskName = this.getTaskName();
        final Object other$taskName = other.getTaskName();
        if (this$taskName == null ? other$taskName != null : !this$taskName.equals(other$taskName)) return false;
        if (this.getTaskServiceId() != other.getTaskServiceId()) return false;
        final Object this$environment = this.getEnvironment();
        final Object other$environment = other.getEnvironment();
        if (this$environment == null ? other$environment != null : !this$environment.equals(other$environment))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        final Object $nodeUniqueId = this.getNodeUniqueId();
        result = result * PRIME + ($nodeUniqueId == null ? 43 : $nodeUniqueId.hashCode());
        final Object $taskName = this.getTaskName();
        result = result * PRIME + ($taskName == null ? 43 : $taskName.hashCode());
        result = result * PRIME + this.getTaskServiceId();
        final Object $environment = this.getEnvironment();
        result = result * PRIME + ($environment == null ? 43 : $environment.hashCode());
        return result;
    }

    public String toString() {
        return "ServiceId(uniqueId=" + this.getUniqueId() + ", nodeUniqueId=" + this.getNodeUniqueId() + ", taskName=" + this.getTaskName() + ", taskServiceId=" + this.getTaskServiceId() + ", environment=" + this.getEnvironment() + ")";
    }
}