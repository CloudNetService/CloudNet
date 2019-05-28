package de.dytanic.cloudnet.ext.cloudperms.node;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.cloudperms.node.listener.ConfigurationUpdateListener;
import de.dytanic.cloudnet.ext.cloudperms.node.listener.IncludePluginListener;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

public final class CloudNetCloudPermissionsModule extends NodeCloudNetModule {

  private static final Type LIST_STRING = new TypeToken<List<String>>() {
  }.getType();

  @Getter
  private static CloudNetCloudPermissionsModule instance;

  @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
  public void init() {
    instance = this;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void initConfig() {
    getConfig().getBoolean("enabled", true);
    getConfig().get("excludedGroups", LIST_STRING, Collections.EMPTY_LIST);
    saveConfig();
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    registerListeners(new IncludePluginListener(),
      new ConfigurationUpdateListener());
  }

  /*= ------------------------------------------------- =*/

  public List<String> getExcludedGroups() {
    return getConfig().get("excludedGroups", LIST_STRING);
  }
}