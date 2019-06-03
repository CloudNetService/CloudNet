package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class VelocityChannelMessageReceiveEvent extends
    VelocityCloudNetEvent {

  @Getter
  private final String channel, message;

  @Getter
  private final JsonDocument data;
}