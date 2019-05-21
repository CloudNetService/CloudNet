package de.dytanic.cloudnet.wrapper.event;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.wrapper.Wrapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * This event is called before the actual Jar archive of the application is searched.
 * The environment is already defined.
 *
 * @see ServiceEnvironmentType
 * @see Event
 */
@Getter
@AllArgsConstructor
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
    @Setter
    private ServiceEnvironmentType environmentType;

}