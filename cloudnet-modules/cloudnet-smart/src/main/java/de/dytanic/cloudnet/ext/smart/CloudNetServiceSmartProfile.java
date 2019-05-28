package de.dytanic.cloudnet.ext.smart;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class CloudNetServiceSmartProfile extends
    BasicJsonDocPropertyable {

  private final UUID uniqueId;

  private final AtomicInteger autoStopCount;

}