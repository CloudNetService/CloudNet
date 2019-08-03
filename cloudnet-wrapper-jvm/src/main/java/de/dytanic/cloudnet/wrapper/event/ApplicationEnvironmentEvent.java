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
 */
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

    @Deprecated(since = "3.0.1", forRemoval = true)
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

    /**
     * Set the environment which will be used to find the jar file
     * Since the jar file may not be into the classpath, by using this your application might get broken
     * Define your environment before starting the service, by using events from {@link de.dytanic.cloudnet.event.service }
     * @param environmentType type of environment for this service
     */
    @Deprecated(since = "3.0.1", forRemoval = true)
    public void setEnvironmentType(ServiceEnvironmentType environmentType) {
        this.environmentType = environmentType;
    }
}