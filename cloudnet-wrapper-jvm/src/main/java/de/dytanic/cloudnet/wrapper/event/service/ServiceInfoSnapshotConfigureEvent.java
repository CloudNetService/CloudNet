package de.dytanic.cloudnet.wrapper.event.service;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The event is called when a new ServiceInfoSnapshot has been created to update this service.
 * With the getProperties() Method by ServiceInfoSnapshot you can added optional properties.
 * <p>
 * This Event will called every update with the Wrapper API
 *
 * @see ServiceInfoSnapshot
 * @see Event
 */
@Getter
@RequiredArgsConstructor
public class ServiceInfoSnapshotConfigureEvent extends Event {

    /**
     * The new created serviceInfoSnapshot, which was created with the Wrapper.getInstance().publishServiceInfoUpdate()
     * Method.
     */
    private final ServiceInfoSnapshot serviceInfoSnapshot;

}