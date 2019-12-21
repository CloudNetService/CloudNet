package de.dytanic.cloudnet.database.h2;

import de.dytanic.cloudnet.database.sql.SQLDatabase;

public final class H2Database extends SQLDatabase {
    public H2Database(H2DatabaseProvider databaseProvider, String name) {
        super(databaseProvider, name);
    }
}