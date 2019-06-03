package de.dytanic.cloudnet.ext.signs.node;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.SignConfigurationReaderAndWriter;
import de.dytanic.cloudnet.ext.signs.SignConstants;
import de.dytanic.cloudnet.ext.signs.node.command.CommandSigns;
import de.dytanic.cloudnet.ext.signs.node.http.V1SignConfigurationHttpHandler;
import de.dytanic.cloudnet.ext.signs.node.listener.CloudNetSignsModuleListener;
import de.dytanic.cloudnet.ext.signs.node.listener.IncludePluginListener;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import java.io.File;
import java.util.Collection;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;

public final class CloudNetSignsModule extends NodeCloudNetModule {

  private static final String SIGN_STORE_DOCUMENT = "signs_store";

  @Getter
  private static CloudNetSignsModule instance;

  @Getter
  @Setter
  private SignConfiguration signConfiguration;

  @Getter
  private File configurationFile;

  public CloudNetSignsModule() {
    instance = this;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void createConfigurationOrUpdate() {
    configurationFile = new File(getModuleWrapper().getDataFolder(),
        "config.json");
    signConfiguration = SignConfigurationReaderAndWriter
        .read(configurationFile);
  }

  @ModuleTask(order = 125, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    registerListener(new IncludePluginListener());
    registerListener(new CloudNetSignsModuleListener());
  }

  @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
  public void registerCommands() {
    registerCommand(new CommandSigns());
  }

  @ModuleTask(order = 123, event = ModuleLifeCycle.STARTED)
  public void registerHttpHandlers() {
    getHttpServer().registerHandler("/api/v1/modules/signs/config",
        new V1SignConfigurationHttpHandler(
            "cloudnet.http.v1.modules.signs.config"));
  }

  public void addSignToFile(Sign sign) {
    Validate.checkNotNull(sign);

    Collection<Sign> signs = this.loadSigns();
    signs.add(sign);

    this.write(signs);
  }

  public void removeSignToFile(Sign sign) {
    Validate.checkNotNull(sign);

    Collection<Sign> signs = this.loadSigns();

    Sign first = Iterables.first(signs, new Predicate<Sign>() {
      @Override
      public boolean test(Sign s) {
        return sign.getSignId() == s.getSignId();
      }
    });

    if (first != null) {
      signs.remove(first);
    }

    signs.remove(first);
    this.write(signs);
  }

  public Collection<Sign> loadSigns() {
    IDatabase database = getDatabaseProvider()
        .getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME);
    JsonDocument document = database.get(SIGN_STORE_DOCUMENT);

    return document != null ? document
        .get("signs", SignConstants.COLLECTION_SIGNS, Iterables.newArrayList())
        : Iterables.newArrayList();
  }

  public void write(Collection<Sign> signs) {
    Validate.checkNotNull(signs);

    IDatabase database = getDatabaseProvider()
        .getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME);
    JsonDocument document = database.get(SIGN_STORE_DOCUMENT);

    if (document == null) {
      document = new JsonDocument();
    }

    database.update(SIGN_STORE_DOCUMENT, document.append("signs", signs));
  }
}