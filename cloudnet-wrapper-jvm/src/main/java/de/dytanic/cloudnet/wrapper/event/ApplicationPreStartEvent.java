package de.dytanic.cloudnet.wrapper.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.io.File;
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
    private Manifest manifest;

    /**
     * The file of the original application
     */
    private File applicationFile;

    /**
     * The arguments for the main method of the application
     */
    private final Collection<String> arguments;

    /**
     * @deprecated the manifest of the application file is not available in the wrapper anymore and has been replaced by the whole application file
     */
    @Deprecated
    public ApplicationPreStartEvent(Wrapper cloudNetWrapper, Class<?> clazz, Manifest manifest, Collection<String> arguments) {
        this.cloudNetWrapper = cloudNetWrapper;
        this.clazz = clazz;
        this.manifest = manifest;
        this.arguments = arguments;
    }

    public ApplicationPreStartEvent(Wrapper cloudNetWrapper, Class<?> clazz, File applicationFile, Collection<String> arguments) {
        this.cloudNetWrapper = cloudNetWrapper;
        this.clazz = clazz;
        this.applicationFile = applicationFile;
        this.arguments = arguments;
    }

    public Wrapper getCloudNetWrapper() {
        return this.cloudNetWrapper;
    }

    public Class<?> getClazz() {
        return this.clazz;
    }

    /**
     * @deprecated the manifest of the application file is not available in the wrapper anymore,
     * use {@link ApplicationPreStartEvent#getApplicationFile()} to get the file of the manifest
     */
    @Deprecated
    public Manifest getManifest() {
        return manifest;
    }

    public File getApplicationFile() {
        return applicationFile;
    }

    public Collection<String> getArguments() {
        return this.arguments;
    }
}
