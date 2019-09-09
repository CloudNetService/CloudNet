package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

import java.util.UUID;

public class PacketServerDatabaseAction implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        if (packet.getHeader().contains("message")) {
            AbstractDatabaseProvider databaseProvider = CloudNet.getInstance().getDatabaseProvider();
            String message = packet.getHeader().getString("message");

            if (packet.getHeader().contains("database")) {
                String databaseName = packet.getHeader().getString("database");

                IDatabase database = databaseProvider.getDatabase(databaseName);

                // actions for a specific key in the database
                if (packet.getHeader().contains("key")) {
                    String key = packet.getHeader().getString("key");
                    switch (message) {
                        case "contains":
                            this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(),
                                    new byte[]{
                                            (byte) (database.contains(key) ? 1 : 0)
                                    }
                            );
                            return;
                        case "insert":
                            this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(),
                                    new byte[]{
                                            (byte) (database.insert(key, packet.getHeader().getDocument("value")) ? 1 : 0)
                                    }
                            );
                            return;
                        case "delete":
                            this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(),
                                    new byte[]{
                                            (byte) (database.delete(key) ? 1 : 0)
                                    }
                            );
                            return;
                        case "update":
                            this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(),
                                    new byte[]{
                                            (byte) (database.update(key, packet.getHeader().getDocument("value")) ? 1 : 0)
                                    }
                            );
                            return;
                    }
                }
                if (message.equals("get")) {
                    if (packet.getHeader().contains("filters")) {
                        JsonDocument filters = packet.getHeader().getDocument("filters");
                        this.sendResponse(channel, packet.getUniqueId(),
                                new JsonDocument()
                                        .append("matches", database.get(filters))
                        );
                        return;
                    } else if (packet.getHeader().contains("key")) {
                        this.sendResponse(channel, packet.getUniqueId(),
                                new JsonDocument()
                                        .append("match", database.get(packet.getHeader().getString("key")))
                        );
                        return;
                    } else if (packet.getHeader().contains("name") && packet.getHeader().contains("value")) {
                        this.sendResponse(channel, packet.getUniqueId(),
                                new JsonDocument()
                                        .append("matches", database.get(packet.getHeader().getString("name"), packet.getHeader().getString("value")))
                        );
                        return;
                    }
                }
                // actions for a specific key in the database 

                // actions for the specific database WITH NO SPECIFIC KEY
                if (message.equals("keys")) {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument()
                                    .append("keys", database.keys())
                    );
                    return;
                }
                if (message.equals("entries")) {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument()
                                    .append("entries", database.entries())
                    );
                    return;
                }
                if (message.equals("clear")) {
                    database.clear();
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    return;
                }
                if (message.equals("documents")) {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument().append("documents", database.documents()));
                    return;
                }
                if (message.equals("contains")) {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(),
                            new byte[]{
                                    (byte) (databaseProvider.containsDatabase(databaseName) ? 1 : 0)
                            }
                    );
                    return;
                }
                if (message.equals("delete")) {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(),
                            new byte[]{
                                    (byte) (databaseProvider.deleteDatabase(databaseName) ? 1 : 0)
                            }
                    );
                    return;
                }
                if (message.equals("close")) {
                    database.close();
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    return;
                }
                // actions for the specific database WITH NO SPECIFIC KEY

            }


            // actions for the database provider WITHOUT a specific database
            if (message.equals("databases")) {
                this.sendResponse(channel, packet.getUniqueId(), new JsonDocument().append("databases", databaseProvider.getDatabaseNames()));
                return;
            }
            // actions for the database provider WITHOUT a specific database
        }
    }

    private void sendResponse(INetworkChannel channel, UUID uniqueId, JsonDocument header) {
        sendResponse(channel, uniqueId, header, null);
    }

    private void sendResponse(INetworkChannel channel, UUID uniqueId, JsonDocument header, byte[] body) {
        channel.sendPacket(new Packet(PacketConstants.INTERNAL_CALLABLE_CHANNEL, uniqueId, header, body));
    }

    private void sendEmptyResponse(INetworkChannel channel, UUID uniqueId) {
        sendResponse(channel, uniqueId, new JsonDocument(), null);
    }

}
