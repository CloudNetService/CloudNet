package de.dytanic.cloudnet.driver.event.events.channel;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ChannelMessageReceiveEvent extends Event {

  private final String channel;

  private final String message;

  private final JsonDocument data;

}