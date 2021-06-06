package de.dytanic.cloudnet.ext.smart;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class CloudNetServiceSmartProfile extends BasicJsonDocPropertyable {

  private final UUID uniqueId;

  private final AtomicInteger autoStopCount;

  public CloudNetServiceSmartProfile(UUID uniqueId, AtomicInteger autoStopCount) {
    this.uniqueId = uniqueId;
    this.autoStopCount = autoStopCount;
  }

  public UUID getUniqueId() {
    return this.uniqueId;
  }

  public AtomicInteger getAutoStopCount() {
    return this.autoStopCount;
  }

}
