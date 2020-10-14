package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.GoMint;
import io.gomint.event.Event;

/**
 * All CloudNet events will mostly fire asynchronously, depending on how they were triggered.
 * Check {@link GoMint#isMainThread()} and treat the event appropriately.
 */
abstract class GoMintCloudNetEvent extends Event {

    public final CloudNetDriver getDriver() {
        return CloudNetDriver.getInstance();
    }

    public final Wrapper getWrapper() {
        return Wrapper.getInstance();
    }

}