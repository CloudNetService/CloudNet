package de.dytanic.cloudnet.driver;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.DefaultTaskScheduler;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.common.registry.DefaultServicesRegistry;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.event.DefaultEventManager;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.module.DefaultModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public abstract class CloudNetDriver {

    private static CloudNetDriver instance;

    protected IPermissionManagement permissionManagement;

    protected final IServicesRegistry servicesRegistry = new DefaultServicesRegistry();

    protected final IEventManager eventManager = new DefaultEventManager();

    protected final IModuleProvider moduleProvider = new DefaultModuleProvider();

    protected final ITaskScheduler taskScheduler = new DefaultTaskScheduler();
    protected final ILogger logger;
    protected DriverEnvironment driverEnvironment = DriverEnvironment.EMBEDDED;

    public CloudNetDriver(@NotNull ILogger logger) {
        this.logger = logger;
    }

    public static CloudNetDriver getInstance() {
        return CloudNetDriver.instance;
    }

    /**
     * The CloudNetDriver instance won't be null usually, this method is only relevant for tests
     *
     * @return optional CloudNetDriver
     */
    public static Optional<CloudNetDriver> optionalInstance() {
        return Optional.ofNullable(CloudNetDriver.instance);
    }

    protected static void setInstance(@NotNull CloudNetDriver instance) {
        CloudNetDriver.instance = instance;
    }


    public abstract void start() throws Exception;

    public abstract void stop();


    /**
     * Returns the name of this component. (e.g. Node-1, Lobby-1)
     */
    @NotNull
    public abstract String getComponentName();

    @NotNull
    public abstract CloudServiceFactory getCloudServiceFactory();

    @NotNull
    public abstract ServiceTaskProvider getServiceTaskProvider();

    @NotNull
    public abstract NodeInfoProvider getNodeInfoProvider();

    @NotNull
    public abstract GroupConfigurationProvider getGroupConfigurationProvider();

    @NotNull
    public abstract CloudMessenger getMessenger();

    @NotNull
    public IPermissionManagement getPermissionManagement() {
        Preconditions.checkNotNull(this.permissionManagement, "no permission management available");
        return this.permissionManagement;
    }

    public void setPermissionManagement(@NotNull IPermissionManagement permissionManagement) {
        if (this.permissionManagement != null && !this.permissionManagement.canBeOverwritten() && !this.permissionManagement.getClass().getName().equals(permissionManagement.getClass().getName())) {
            throw new IllegalStateException("Current permission management (" + this.permissionManagement.getClass().getName() + ") cannot be overwritten by " + permissionManagement.getClass().getName());
        }

        this.permissionManagement = permissionManagement;
    }

    /**
     * Returns a new service specific CloudServiceProvider
     *
     * @param name the name of the service
     * @return the new instance of the {@link SpecificCloudServiceProvider}
     */
    @NotNull
    public abstract SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name);

    /**
     * Returns a new service specific CloudServiceProvider
     *
     * @param uniqueId the uniqueId of the service
     * @return the new instance of the {@link SpecificCloudServiceProvider}
     */
    @NotNull
    public abstract SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId);

    /**
     * Returns a new service specific CloudServiceProvider
     *
     * @param serviceInfoSnapshot the info of the service to create a provider for
     * @return the new instance of the {@link SpecificCloudServiceProvider}
     */
    @NotNull
    public abstract SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);

    /**
     * Returns the general CloudServiceProvider
     *
     * @return the instance of the {@link GeneralCloudServiceProvider}
     */
    @NotNull
    public abstract GeneralCloudServiceProvider getCloudServiceProvider();

    @NotNull
    public abstract INetworkClient getNetworkClient();

    @NotNull
    public abstract ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync();

    @NotNull
    public abstract ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(@NotNull String serviceName);

    public abstract Collection<ServiceTemplate> getLocalTemplateStorageTemplates();

    public abstract Collection<ServiceTemplate> getTemplateStorageTemplates(@NotNull String serviceName);

    public abstract void setGlobalLogLevel(@NotNull LogLevel logLevel);

    public abstract void setGlobalLogLevel(int logLevel);

    public abstract Pair<Boolean, String[]> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId, @NotNull String commandLine);

    @NotNull
    public abstract ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId, @NotNull String commandLine);

    /**
     * Fetches the PID of this process.
     *
     * @return the PID as an int or -1, if it couldn't be fetched
     */
    public int getOwnPID() {
        return ProcessSnapshot.getOwnPID();
    }

    @NotNull
    public IServicesRegistry getServicesRegistry() {
        return this.servicesRegistry;
    }

    @NotNull
    public IEventManager getEventManager() {
        return this.eventManager;
    }

    @NotNull
    public IModuleProvider getModuleProvider() {
        return this.moduleProvider;
    }

    @NotNull
    public ITaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull
    public ILogger getLogger() {
        return this.logger;
    }

    @NotNull
    public DriverEnvironment getDriverEnvironment() {
        return this.driverEnvironment;
    }

}
