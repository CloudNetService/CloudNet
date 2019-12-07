package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class PacketServerAuthorizationResponseListener implements PacketListener {

    private final ReentrantLock lock;

    private final Condition condition;

    private boolean result;

    public PacketServerAuthorizationResponseListener(ReentrantLock lock, Condition condition) {
        this.lock = lock;
        this.condition = condition;
    }

    @Override
    public void handle(INetworkChannel channel, Packet packet) {
        if (packet.getHeader().contains("access") && packet.getHeader().contains("text")) {
            result = packet.getHeader().getBoolean("access");

            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    public boolean isResult() {
        return this.result;
    }
}