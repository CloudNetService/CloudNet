package de.dytanic.cloudnet.ext.database.mysql.util;

import de.dytanic.cloudnet.driver.network.HostAndPort;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MySQLConnectionEndpoint)) return false;
        final MySQLConnectionEndpoint other = (MySQLConnectionEndpoint) o;
        if (this.isUseSsl() != other.isUseSsl()) return false;
        final Object this$database = this.getDatabase();
        final Object other$database = other.getDatabase();
        if (this$database == null ? other$database != null : !this$database.equals(other$database)) return false;
        final Object this$address = this.getAddress();
        final Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isUseSsl() ? 79 : 97);
        final Object $database = this.getDatabase();
        result = result * PRIME + ($database == null ? 43 : $database.hashCode());
        final Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        return result;
    }

    public String toString() {
        return "MySQLConnectionEndpoint(useSsl=" + this.isUseSsl() + ", database=" + this.getDatabase() + ", address=" + this.getAddress() + ")";
    }
}