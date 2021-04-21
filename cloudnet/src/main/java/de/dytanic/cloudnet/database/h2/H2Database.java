package de.dytanic.cloudnet.database.h2;

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
}