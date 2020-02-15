package de.dytanic.cloudnet.ext.bridge.sponge.event;


import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;

abstract class SpongeBridgeEvent implements Event {

    public final CloudNetDriver getDriver() {
        return CloudNetDriver.getInstance();
    }

    public final Wrapper getWrapper() {
        return Wrapper.getInstance();
    }

    @NotNull
    @Override
    public Cause getCause() {
        return Cause.builder().build(EventContext.builder().build());
    }
}