package de.dytanic.cloudnet.ext.storage.ftp;

import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

public final class CloudNetStorageFTPModule extends NodeCloudNetModule {

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void initConfiguration() {
    getConfig().getBoolean("ssl", true);
    getConfig()
      .get("address", HostAndPort.class, new HostAndPort("127.0.0.1", 21));

    getConfig().getString("storage", "ftp");
    getConfig().getString("username", "root");
    getConfig().getString("password", "123456");
    getConfig().getInt("bufferSize", 8192);
    getConfig().getString("baseDirectory", "/home/cloudnet");

    saveConfig();
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
  public void registerStorage() {
    registerTemplateStorage(getConfig().getString("storage"),
      new FTPTemplateStorage(this.getConfig()));
  }
}