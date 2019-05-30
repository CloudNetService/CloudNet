package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.HandlerList;

@RequiredArgsConstructor
public final class BukkitChannelMessageReceiveEvent extends
  BukkitCloudNetEvent {

  @Getter
  private static HandlerList handlerList = new HandlerList();

  @Getter
  private final String channel, message;

  @Getter
  private final JsonDocument data;

  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }
}