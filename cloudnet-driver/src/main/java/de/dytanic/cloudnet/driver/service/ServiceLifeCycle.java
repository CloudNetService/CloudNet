package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;

import java.util.UUID;

/**
 * The current state of a service.
 */
public enum ServiceLifeCycle {

    /**
     * This will only be for a very short time after the creation of a service if the {@code CloudServicePrePrepareEvent}
     * in the node isn't cancelled. After that event has been called, CloudNet will only copy the SSL certificates if
     * necessary and directly switch to the {@code PREPARED} state after it.
     */
    DEFINED,
    /**
     * This is the state directly after {@code DEFINED} or after {@code STOPPED} (only if autoDeleteOnStop is disabled for
     * the service). It will be prepared until it is changed to {@code RUNNING} by {@link SpecificCloudServiceProvider#start()}.
     */
    PREPARED,
    /**
     * This is the state after {@code PREPARED}. It is invoked by {@link SpecificCloudServiceProvider#start()}. It will
     * be running until the process of the service has exited.
     */
    RUNNING,
    /**
     * This is the state after {@code RUNNING}. It is invoked by exiting the process. This will only be for a very short time
     * after the process has exited. There are two possibilities for the next state:
     * - If autoDeleteOnStop is enabled, the state will be switched to {@code DELETED}.
     * - If autoDeleteOnStop is disabled, the state will be switched to {@code PREPARED}.
     */
    STOPPED,
    /**
     * This is the state after {@code STOPPED}. When this state is set, the service is no more registered in the cloud and
     * methods like {@link GeneralCloudServiceProvider#getCloudService(UUID)} won't return this service anymore.
     */
    DELETED,

}