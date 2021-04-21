package de.dytanic.cloudnet.database.h2;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.sql.SQLDatabase;

import java.util.concurrent.ExecutorService;

public final class H2Database extends SQLDatabase {

    public H2Database(H2DatabaseProvider databaseProvider, String name, ExecutorService executorService) {
        super(databaseProvider, name, executorService);
    }

    @Override
    public boolean isSynced() {
        return false;
    }

    @Override
    public boolean insertOrUpdate(String key, JsonDocument document) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(document);

        return this.databaseProvider.executeUpdate(
                "MERGE INTO `" + this.name + "` (" + TABLE_COLUMN_KEY + "," + TABLE_COLUMN_VALUE + ") VALUES (?, ?);",
                key, document.toString()
        ) != -1;
    }
}