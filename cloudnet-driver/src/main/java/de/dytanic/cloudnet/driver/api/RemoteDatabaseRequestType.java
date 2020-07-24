package de.dytanic.cloudnet.driver.api;

public enum RemoteDatabaseRequestType {

    CONTAINS_DATABASE(false),
    DELETE_DATABASE(false),
    GET_DATABASES(false),

    DATABASE_INSERT(true),
    DATABASE_UPDATE(true),
    DATABASE_CONTAINS(true),
    DATABASE_DELETE(true),
    DATABASE_GET_BY_KEY(true),
    DATABASE_GET_BY_FIELD(true),
    DATABASE_GET_BY_FILTERS(true),
    DATABASE_KEYS(true),
    DATABASE_DOCUMENTS(true),
    DATABASE_ENTRIES(true),
    DATABASE_CLEAR(true),
    DATABASE_CLOSE(true),
    DATABASE_COUNT_DOCUMENTS(true);

    private final boolean databaseSpecific;

    RemoteDatabaseRequestType(boolean databaseSpecific) {
        this.databaseSpecific = databaseSpecific;
    }

    public boolean isDatabaseSpecific() {
        return this.databaseSpecific;
    }
}
