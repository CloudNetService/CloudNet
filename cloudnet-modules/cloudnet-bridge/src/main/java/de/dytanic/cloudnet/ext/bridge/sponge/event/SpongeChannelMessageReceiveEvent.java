package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SpongeChannelMessageReceiveEvent extends SpongeCloudNetEvent {

    @Getter
    private final String channel, message;

    @Getter
    private final JsonDocument data;
}