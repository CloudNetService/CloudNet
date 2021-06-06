package de.dytanic.cloudnet.module;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.conf.IConfiguration;
import de.dytanic.cloudnet.conf.IConfigurationRegistry;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.template.TemplateStorage;

public abstract class NodeCloudNetModule extends DriverModule {

  public final void registerCommand(Command command) {
    this.getCloudNet().getCommandMap().registerCommand(command);
  }

  public final <T extends TemplateStorage> T registerTemplateStorage(String serviceName, T templateStorage) {
    Preconditions.checkNotNull(serviceName);
    Preconditions.checkNotNull(templateStorage);

    this.getRegistry().registerService(TemplateStorage.class, serviceName, templateStorage);

    return templateStorage;
  }

  public final <T extends AbstractDatabaseProvider> T registerDatabaseProvider(String serviceName, T databaseProvider) {
    Preconditions.checkNotNull(serviceName);
    Preconditions.checkNotNull(databaseProvider);

    this.getRegistry().registerService(AbstractDatabaseProvider.class, serviceName, databaseProvider);

    return databaseProvider;
  }

  public final IHttpServer registerHttpHandler(String path, IHttpHandler... httpHandlers) {
    Preconditions.checkNotNull(path);
    Preconditions.checkNotNull(httpHandlers);

    return this.getHttpServer().registerHandler(path, httpHandlers);
  }

  public final AbstractDatabaseProvider getDatabaseProvider() {
    return this.getCloudNet().getDatabaseProvider();
  }

  public final IHttpServer getHttpServer() {
    return this.getCloudNet().getHttpServer();
  }

  public final NetworkClusterNode getIdentity() {
    return this.getCloudNetConfig().getIdentity();
  }

  public final IConfiguration getCloudNetConfig() {
    return CloudNet.getInstance().getConfig();
  }

  public final IConfigurationRegistry getCloudRegistry() {
    return CloudNet.getInstance().getConfigurationRegistry();
  }

  public final CloudNet getCloudNet() {
    return CloudNet.getInstance();
  }
}
