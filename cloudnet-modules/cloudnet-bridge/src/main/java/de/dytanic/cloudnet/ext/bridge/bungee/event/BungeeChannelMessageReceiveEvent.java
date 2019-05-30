package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BungeeChannelMessageReceiveEvent extends
  BungeeCloudNetEvent {

  @Getter
  private final String channel, message;

  @Getter
  private final JsonDocument data;
}