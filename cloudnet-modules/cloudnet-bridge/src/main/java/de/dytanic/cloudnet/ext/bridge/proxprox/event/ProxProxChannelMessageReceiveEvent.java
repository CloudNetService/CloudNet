package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ProxProxChannelMessageReceiveEvent extends
    ProxProxCloudNetEvent {

  @Getter
  private final String channel, message;

  @Getter
  private final JsonDocument data;
}