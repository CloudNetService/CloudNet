package de.dytanic.cloudnet.ext.storage.ftp;

import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPCredentials;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPType;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import de.dytanic.cloudnet.template.ITemplateStorage;

import java.util.Arrays;

public final class CloudNetStorageFTPModule extends NodeCloudNetModule {

    @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
    public void initConfiguration() {
        if (getConfig().contains("ssl")) {
            getConfig().remove("ssl");
        }
        getConfig().get("type", FTPType.class, FTPType.FTP);
        getConfig().get("address", HostAndPort.class, new HostAndPort("127.0.0.1", 21));

        getConfig().getString("storage", "ftp");
        getConfig().getString("username", "root");
        getConfig().getString("password", "123456");
        getConfig().getInt("bufferSize", 8192);
        getConfig().getString("baseDirectory", "/home/cloudnet");

        saveConfig();
    }

    @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
    public void registerStorage() {
        FTPType ftpType = getConfig().get("type", FTPType.class);
        if (ftpType == null) {
            System.err.println("FTP type in the config doesn't exist, disabling FTP template storage");
            return;
        }
        System.out.println("Using " + ftpType + " for FTP connection (Available types: " + Arrays.toString(FTPType.values()) + ")");
        FTPCredentials credentials = getConfig().toInstanceOf(FTPCredentials.class);
        ITemplateStorage templateStorage = ftpType.createNewTemplateStorage(getConfig().getString("storage"), credentials);
        registerTemplateStorage(getConfig().getString("storage"), templateStorage);
    }
}