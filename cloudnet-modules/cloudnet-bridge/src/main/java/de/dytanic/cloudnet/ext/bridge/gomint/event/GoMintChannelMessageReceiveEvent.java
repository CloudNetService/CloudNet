package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class GoMintChannelMessageReceiveEvent extends
    GoMintCloudNetEvent {

  @Getter
  private final String channel, message;

  @Getter
  private final JsonDocument data;
}