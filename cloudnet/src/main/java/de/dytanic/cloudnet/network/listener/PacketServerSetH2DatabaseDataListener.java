package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.h2.H2Database;
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;

import java.lang.reflect.Type;
import java.util.Map;

public final class PacketServerSetH2DatabaseDataListener implements IPacketListener {

    private static final Type TYPE = new TypeToken<Map<String, Map<String, JsonDocument>>>() {
    }.getType();

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        if (CloudNet.getInstance().getDatabaseProvider() instanceof H2DatabaseProvider && packet.getHeader().contains("set_h2db")) {
            Map<String, Map<String, JsonDocument>> documents = packet.getHeader().get("documents", TYPE);

            H2DatabaseProvider databaseProvider = getH2DatabaseProvider();

            for (String name : databaseProvider.getDatabaseNames()) {
                if (!documents.containsKey(name)) {
                    databaseProvider.deleteDatabase(name);
                    continue;
                }

                H2Database database = databaseProvider.getDatabase(name);

                try {
                    database.clear0();
                } catch (Exception ignored) {
                }
            }

            for (Map.Entry<String, Map<String, JsonDocument>> db : documents.entrySet()) {
                H2Database database = databaseProvider.getDatabase(db.getKey());

                for (Map.Entry<String, JsonDocument> entry : documents.get(db.getKey()).entrySet())
                    database.insert0(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, Map<String, JsonDocument>> entry : documents.entrySet())
                entry.getValue().clear();

            documents.clear();
        }
    }

    public H2DatabaseProvider getH2DatabaseProvider() {
        return (H2DatabaseProvider) CloudNet.getInstance().getDatabaseProvider();
    }
}