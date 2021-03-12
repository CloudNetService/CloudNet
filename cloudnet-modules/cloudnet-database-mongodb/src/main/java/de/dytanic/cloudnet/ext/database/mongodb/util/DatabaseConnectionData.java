package de.dytanic.cloudnet.ext.database.mongodb.util;

import com.google.gson.Gson;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseConnectionData {
    private String host;
    private int port;
    private String database;
    private String user;
    private String password;

    public static DatabaseConnectionData fromJsonDocument(JsonDocument document) {
        DatabaseConnectionData databaseConnectionData = new Gson().fromJson(document.toJson(), DatabaseConnectionData.class);
        return databaseConnectionData;
    }

    public String toConnectionString() {
        return "mongodb://" + user + ":" + password + "@" + host + ":" + port + "/" + database;
    }

}
