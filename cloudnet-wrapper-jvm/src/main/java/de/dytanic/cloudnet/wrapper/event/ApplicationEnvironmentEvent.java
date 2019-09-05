package de.dytanic.cloudnet.wrapper.event;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.wrapper.Wrapper;

/**
 * This event is called before the actual Jar archive of the application is searched.
 * The environment is already defined.
 *
 * @see ServiceEnvironmentType
 * @see Event
 * @deprecated the searching of the application jar archive has been moved to the node, this event is not being used anymore
 */
@Deprecated
public final class ApplicationEnvironmentEvent extends Event {

    /**
     * The current singleton instance of the Wrapper class
     *
     * @see Wrapper
     */
    private final Wrapper cloudNetWrapper;

    /**
     * The application environment type for the instance, which the service will start with
     *
     * @see ServiceEnvironmentType
     */
    private ServiceEnvironmentType environmentType;

    public ApplicationEnvironmentEvent(Wrapper cloudNetWrapper, ServiceEnvironmentType environmentType) {
        this.cloudNetWrapper = cloudNetWrapper;
        this.environmentType = environmentType;
    }

    public Wrapper getCloudNetWrapper() {
        return this.cloudNetWrapper;
    }

    public ServiceEnvironmentType getEnvironmentType() {
        return this.environmentType;
    }

    public void setEnvironmentType(ServiceEnvironmentType environmentType) {
        this.environmentType = environmentType;
    }

}
