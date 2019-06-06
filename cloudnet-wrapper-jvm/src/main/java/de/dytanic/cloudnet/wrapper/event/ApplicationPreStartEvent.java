package de.dytanic.cloudnet.wrapper.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Collection;
import java.util.jar.Manifest;

/**
 * This event is only interesting for wrapper modules.
 * It is called before the actual program is started in a new thread.
 *
 * @see DriverEvent
 */
public final class ApplicationPreStartEvent extends DriverEvent {

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
     * The manifest properties of the jar archive by the original application
     *
     * @see Manifest
     */
    private final Manifest manifest;

    /**
     * The arguments for the main method of the application
     */
    private final Collection<String> arguments;

    public ApplicationPreStartEvent(Wrapper cloudNetWrapper, Class<?> clazz, Manifest manifest, Collection<String> arguments) {
        this.cloudNetWrapper = cloudNetWrapper;
        this.clazz = clazz;
        this.manifest = manifest;
        this.arguments = arguments;
    }

    public Wrapper getCloudNetWrapper() {
        return this.cloudNetWrapper;
    }

    public Class<?> getClazz() {
        return this.clazz;
    }

    public Manifest getManifest() {
        return this.manifest;
    }

    public Collection<String> getArguments() {
        return this.arguments;
    }
}
