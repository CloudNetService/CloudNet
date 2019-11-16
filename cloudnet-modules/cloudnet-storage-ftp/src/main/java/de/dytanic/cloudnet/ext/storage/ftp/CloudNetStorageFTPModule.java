package de.dytanic.cloudnet.ext.storage.ftp;

import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPCredentials;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPType;
import de.dytanic.cloudnet.ext.storage.ftp.storage.queue.FTPQueueStorage;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

import java.util.Arrays;

public final class CloudNetStorageFTPModule extends NodeCloudNetModule {

    private FTPQueueStorage templateStorage;

    @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
    public void initConfiguration() {
        if (super.getConfig().contains("ssl")) {
            super.getConfig().remove("ssl");
        }

        super.getConfig().get("type", FTPType.class, FTPType.FTP);
        super.getConfig().get("address", HostAndPort.class, new HostAndPort("127.0.0.1", 21));

        super.getConfig().getString("storage", "ftp");
        super.getConfig().getString("username", "root");
        super.getConfig().getString("password", "123456");
        super.getConfig().getInt("bufferSize", 8192);
        super.getConfig().getString("baseDirectory", "/home/cloudnet");

        super.saveConfig();
    }

    @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
    public void registerStorage() {
        FTPType ftpType = super.getConfig().get("type", FTPType.class);

        if (ftpType == null) {
            super.getModuleWrapper().stopModule();

            throw new IllegalArgumentException("Invalid ftp type! Available types: " + Arrays.toString(FTPType.values()));
        }

        String storageName = super.getConfig().getString("storage");
        FTPCredentials credentials = super.getConfig().toInstanceOf(FTPCredentials.class);

        this.templateStorage = new FTPQueueStorage(ftpType.createNewTemplateStorage(storageName, credentials));
        super.registerTemplateStorage(storageName, this.templateStorage);

        Thread ftpQueueThread = new Thread(this.templateStorage, "FTP queue worker");
        ftpQueueThread.setDaemon(true);
        ftpQueueThread.start();
    }

    @ModuleTask(event = ModuleLifeCycle.STOPPED)
    public void unregisterStorage() {
        if (this.templateStorage != null) {
            try {
                this.templateStorage.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

}