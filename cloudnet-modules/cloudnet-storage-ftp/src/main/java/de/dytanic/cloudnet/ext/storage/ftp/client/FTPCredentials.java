package de.dytanic.cloudnet.ext.storage.ftp.client;

import de.dytanic.cloudnet.driver.network.HostAndPort;

public class FTPCredentials {

    private HostAndPort address;
    private String username, password;
    private String baseDirectory;

    public FTPCredentials(HostAndPort address, String username, String password, String baseDirectory) {
        this.address = address;
        this.username = username;
        this.password = password;
        this.baseDirectory = baseDirectory;
    }

    public String getPassword() {
        return this.password;
    }

    public HostAndPort getAddress() {
        return this.address;
    }

    public String getBaseDirectory() {
        return this.baseDirectory;
    }

    public String getUsername() {
        return this.username;
    }
}
