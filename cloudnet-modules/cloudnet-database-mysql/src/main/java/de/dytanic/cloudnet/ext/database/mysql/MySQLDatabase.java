package de.dytanic.cloudnet.ext.database.mysql;

import de.dytanic.cloudnet.database.sql.SQLDatabase;

public final class MySQLDatabase extends SQLDatabase {

    public MySQLDatabase(MySQLDatabaseProvider databaseProvider, String name) {
        super(databaseProvider, name);
    }

}