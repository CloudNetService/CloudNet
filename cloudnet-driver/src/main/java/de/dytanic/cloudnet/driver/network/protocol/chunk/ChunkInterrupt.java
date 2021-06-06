package de.dytanic.cloudnet.driver.network.protocol.chunk;

public final class ChunkInterrupt extends RuntimeException {

  public static final ChunkInterrupt INSTANCE = new ChunkInterrupt();

  private ChunkInterrupt() {
  }

}
