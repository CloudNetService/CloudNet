package de.dytanic.cloudnet.wrapper.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.wrapper.Wrapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This event is only interesting for wrapper modules.
 * It will called if the app is successful started and will called parallel to the application thread.
 *
 * @see DriverEvent
 */
@Getter
@RequiredArgsConstructor
public final class ApplicationPostStartEvent extends DriverEvent {

    /**
     * The current singleton instance of the Wrapper class
     *
     * @see Wrapper
     */
    private final Wrapper cloudNetWrapper;

    /**
     * The class, which is set in the manifest as 'Main-Class' by the archive of the wrapped application
     */
    private final Class<?> clazz;

    /**
     * The application thread, which invoked the main() method of the Main-Class from the application
     */
    private final Thread applicationThread;

    /**
     * The used ClassLoader
     */
    private final ClassLoader classLoader;

}