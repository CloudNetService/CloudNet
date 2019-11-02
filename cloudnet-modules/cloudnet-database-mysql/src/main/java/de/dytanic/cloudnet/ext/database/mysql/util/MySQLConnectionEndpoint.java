package de.dytanic.cloudnet.ext.database.mysql.util;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class MySQLConnectionEndpoint {

    protected final boolean useSsl;

    protected final String database;

    protected final HostAndPort address;

    public MySQLConnectionEndpoint(boolean useSsl, String database, HostAndPort address) {
        this.useSsl = useSsl;
        this.database = database;
        this.address = address;
    }

    public boolean isUseSsl() {
        return this.useSsl;
    }

    public String getDatabase() {
        return this.database;
    }

    public HostAndPort getAddress() {
        return this.address;
    }

}