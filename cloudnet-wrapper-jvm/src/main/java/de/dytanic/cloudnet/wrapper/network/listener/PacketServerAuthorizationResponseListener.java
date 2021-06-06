package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class PacketServerAuthorizationResponseListener implements IPacketListener {

  private final ReentrantLock lock;

  private final Condition condition;

  private boolean result;

  public PacketServerAuthorizationResponseListener(ReentrantLock lock, Condition condition) {
    this.lock = lock;
    this.condition = condition;
  }

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    if (packet.getHeader().contains("access") && packet.getHeader().contains("text")) {
      this.result = packet.getHeader().getBoolean("access");

      try {
        this.lock.lock();
        this.condition.signalAll();
      } finally {
        this.lock.unlock();
      }
    }
  }

  public boolean isResult() {
    return this.result;
  }
}
