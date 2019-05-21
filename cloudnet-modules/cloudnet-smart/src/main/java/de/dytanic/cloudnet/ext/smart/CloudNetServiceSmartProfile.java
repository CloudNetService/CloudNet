package de.dytanic.cloudnet.ext.smart;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class CloudNetServiceSmartProfile extends BasicJsonDocPropertyable {

    private final UUID uniqueId;

    private final AtomicInteger autoStopCount;

}