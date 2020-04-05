package de.dytanic.cloudnet.ext.database.mysql;

import de.dytanic.cloudnet.database.sql.SQLDatabase;

import java.util.concurrent.ExecutorService;

public final class MySQLDatabase extends SQLDatabase {

    public MySQLDatabase(MySQLDatabaseProvider databaseProvider, String name, ExecutorService executorService) {
        super(databaseProvider, name, executorService);
    }

}