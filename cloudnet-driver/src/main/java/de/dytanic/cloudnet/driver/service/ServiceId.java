package de.dytanic.cloudnet.driver.service;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public final class ServiceId {

    private final UUID uniqueId;

    private final String nodeUniqueId, taskName;

    private final int taskServiceId;

    private final ServiceEnvironmentType environment;

    public String getName() {
        return taskName + "-" + taskServiceId;
    }
}