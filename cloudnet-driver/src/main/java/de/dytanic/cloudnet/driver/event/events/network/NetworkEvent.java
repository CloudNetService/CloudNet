package de.dytanic.cloudnet.driver.event.events.network;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class NetworkEvent extends DriverEvent {

  private final INetworkChannel channel;

}