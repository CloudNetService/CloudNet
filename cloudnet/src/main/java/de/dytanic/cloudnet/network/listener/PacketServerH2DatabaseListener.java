package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.database.h2.H2Database;
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.event.database.DatabaseClearEntriesEvent;
import de.dytanic.cloudnet.event.database.DatabaseDeleteEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseInsertEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseUpdateEntryEvent;
import de.dytanic.cloudnet.network.packet.PacketServerH2Database;

public final class PacketServerH2DatabaseListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        if (packet.getHeader().contains("operationType") && packet.getHeader().contains("name"))
            if (CloudNet.getInstance().getDatabaseProvider() instanceof H2DatabaseProvider) {
                H2Database database = (H2Database) CloudNet.getInstance().getDatabaseProvider().getDatabase(packet.getHeader().getString("name"));

                switch (packet.getHeader().get("operationType", PacketServerH2Database.OperationType.class)) {
                    case INSERT:
                        if (packet.getHeader().contains("key") && packet.getHeader().contains("document")) {
                            CloudNetDriver.getInstance().getEventManager().callEvent(
                                    new DatabaseInsertEntryEvent(database, packet.getHeader().getString("key"), packet.getHeader().getDocument("document"))
                            );
                            database.insert0(packet.getHeader().getString("key"), packet.getHeader().getDocument("document"));
                        }
                        break;
                    case UPDATE:
                        if (packet.getHeader().contains("key") && packet.getHeader().contains("document")) {
                            CloudNetDriver.getInstance().getEventManager().callEvent(
                                    new DatabaseUpdateEntryEvent(database, packet.getHeader().getString("key"), packet.getHeader().getDocument("document"))
                            );
                            database.update0(packet.getHeader().getString("key"), packet.getHeader().getDocument("document"));
                        }
                        break;
                    case DELETE:
                        if (packet.getHeader().contains("key")) {
                            CloudNetDriver.getInstance().getEventManager().callEvent(
                                    new DatabaseDeleteEntryEvent(database, packet.getHeader().getString("key"))
                            );
                            database.delete0(packet.getHeader().getString("key"));
                        }
                        break;
                    case CLEAR:
                        CloudNetDriver.getInstance().getEventManager().callEvent(new DatabaseClearEntriesEvent(database));
                        database.clear0();
                        break;
                }
            }
    }
}