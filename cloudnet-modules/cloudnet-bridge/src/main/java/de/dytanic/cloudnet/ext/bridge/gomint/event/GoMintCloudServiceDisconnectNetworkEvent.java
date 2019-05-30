package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class GoMintCloudServiceDisconnectNetworkEvent extends
  GoMintCloudNetEvent {

  @Getter
  private final ServiceInfoSnapshot serviceInfoSnapshot;
}