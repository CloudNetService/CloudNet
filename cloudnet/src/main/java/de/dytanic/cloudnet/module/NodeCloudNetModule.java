package de.dytanic.cloudnet.module;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.Command;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.conf.IConfiguration;
import de.dytanic.cloudnet.conf.IConfigurationRegistry;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.template.ITemplateStorage;

public abstract class NodeCloudNetModule extends DriverModule {

    public final void registerCommand(Command command) {
        getCloudNet().getCommandMap().registerCommand(command);
    }

    public final <T extends ITemplateStorage> T registerTemplateStorage(String serviceName, T templateStorage) {
        Preconditions.checkNotNull(serviceName);
        Preconditions.checkNotNull(templateStorage);

        getRegistry().registerService(ITemplateStorage.class, serviceName, templateStorage);

        return templateStorage;
    }

    public final <T extends AbstractDatabaseProvider> T registerDatabaseProvider(String serviceName, T databaseProvider) {
        Preconditions.checkNotNull(serviceName);
        Preconditions.checkNotNull(databaseProvider);

        getRegistry().registerService(AbstractDatabaseProvider.class, serviceName, databaseProvider);

        return databaseProvider;
    }

    public final IHttpServer registerHttpHandler(String path, IHttpHandler... httpHandlers) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(httpHandlers);

        return getHttpServer().registerHandler(path, httpHandlers);
    }

    public final AbstractDatabaseProvider getDatabaseProvider() {
        return getCloudNet().getDatabaseProvider();
    }

    public final IHttpServer getHttpServer() {
        return getCloudNet().getHttpServer();
    }

    public final NetworkClusterNode getIdentity() {
        return getCloudNetConfig().getIdentity();
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