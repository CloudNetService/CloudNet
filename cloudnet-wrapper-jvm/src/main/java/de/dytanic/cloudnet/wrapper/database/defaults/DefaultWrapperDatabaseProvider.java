package de.dytanic.cloudnet.wrapper.database.defaults;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.wrapper.Wrapper;
import de.dytanic.cloudnet.wrapper.database.IDatabase;
import de.dytanic.cloudnet.wrapper.database.IDatabaseProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class DefaultWrapperDatabaseProvider implements IDatabaseProvider {

    @Override
    public IDatabase getDatabase(String name) {
        return new WrapperDatabase(name, this);
    }

    @Override
    public boolean containsDatabase(String name) {
        return this.containsDatabaseAsync(name).getDef(false);
    }

    @Override
    public boolean deleteDatabase(String name) {
        return this.deleteDatabaseAsync(name).getDef(false);
    }

    @Override
    public Collection<String> getDatabaseNames() {
        return this.getDatabaseNamesAsync().getDef(Collections.emptyList());
    }

    @Override
    public ITask<Boolean> containsDatabaseAsync(String name) {
        return this.executeQuery(name, "contains", response -> response.getSecond()[0] == 1);
    }

    @Override
    public ITask<Boolean> deleteDatabaseAsync(String name) {
        return this.executeQuery(name, "delete", response -> response.getSecond()[0] == 1);
    }

    @Override
    public ITask<Collection<String>> getDatabaseNamesAsync() {
        return this.executeQuery("databases", response -> response.getFirst().get("databases", new TypeToken<Collection<String>>() {
        }.getType()));
    }

    <V> ITask<V> executeQuery(String message, Function<Pair<JsonDocument, byte[]>, V> responseMapper) {
        return this.executeQuery(
                new JsonDocument()
                        .append("message", message),
                responseMapper
        );
    }

    <V> ITask<V> executeQuery(String database, String message, Function<Pair<JsonDocument, byte[]>, V> responseMapper) {
        return this.executeQuery(
                new JsonDocument()
                        .append("database", database)
                        .append("message", message),
                responseMapper
        );
    }

    <V> ITask<V> executeQuery(String database, String message, JsonDocument extras, Function<Pair<JsonDocument, byte[]>, V> responseMapper) {
        return this.executeQuery(
                new JsonDocument()
                        .append("database", database)
                        .append("message", message)
                        .append(extras),
                responseMapper
        );
    }

    <V> ITask<V> executeQuery(JsonDocument header, Function<Pair<JsonDocument, byte[]>, V> responseMapper) {
        return Wrapper.getInstance().sendCallablePacket(
                Wrapper.getInstance().getNetworkClient().getChannels().iterator().next(),
                null,
                header, Packet.EMPTY_PACKET_BYTE_ARRAY, responseMapper
        );
    }

}
