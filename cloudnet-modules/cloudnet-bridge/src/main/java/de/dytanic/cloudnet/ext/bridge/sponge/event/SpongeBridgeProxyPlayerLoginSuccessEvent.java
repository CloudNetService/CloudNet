package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class SpongeBridgeProxyPlayerLoginSuccessEvent extends
  SpongeBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

}