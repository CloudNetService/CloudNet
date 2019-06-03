package de.dytanic.cloudnet.ext.smart;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacketSender;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.ext.smart.command.CommandSmart;
import de.dytanic.cloudnet.ext.smart.listener.CloudNetTickListener;
import de.dytanic.cloudnet.ext.smart.listener.CloudServiceListener;
import de.dytanic.cloudnet.ext.smart.template.TemplateInstaller;
import de.dytanic.cloudnet.ext.smart.util.SmartServiceTaskConfig;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import lombok.Getter;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Getter
public final class CloudNetSmartModule extends NodeCloudNetModule {

    private static final Type SMART_SERVICE_TASKS_CONFIGURATIONS = new TypeToken<Collection<SmartServiceTaskConfig>>() {
    }.getType();

    @Getter
    private static CloudNetSmartModule instance;

    private final Map<UUID, CloudNetServiceSmartProfile> providedSmartServices = Maps.newConcurrentHashMap();

    private final Collection<SmartServiceTaskConfig> smartServiceTaskConfigurations = Iterables.newCopyOnWriteArrayList();

    /*= ------------------------------------------------------------------------------------------------ =*/

    public CloudNetSmartModule()
    {
        instance = this;
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
    public void initConfig()
    {
        load();
    }

    @ModuleTask(order = 64, event = ModuleLifeCycle.STARTED)
    public void registerCommands()
    {
        registerCommand(new CommandSmart());
    }

    @ModuleTask(order = 16, event = ModuleLifeCycle.STARTED)
    public void initListeners()
    {
        registerListeners(new CloudNetTickListener(), new CloudServiceListener());
    }

    /*= ------------------------------------------------------------------------------------------------ =*/

    public int getPercentOfFreeMemory(ServiceTask serviceTask)
    {
        NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot = CloudNet.getInstance().searchLogicNode(serviceTask);

        if (networkClusterNodeInfoSnapshot != null && !networkClusterNodeInfoSnapshot.getNode()
            .getUniqueId().equalsIgnoreCase(getCloudNetConfig().getIdentity().getUniqueId()))

            return (((networkClusterNodeInfoSnapshot.getMaxMemory() - networkClusterNodeInfoSnapshot.getUsedMemory()) * 100) /
                networkClusterNodeInfoSnapshot.getMaxMemory());
        else
            return (((getCloudNet().getConfig().getMaxMemory() - getCloudNet().getCloudServiceManager().getCurrentUsedHeapMemory()) * 100) /
                getCloudNet().getConfig().getMaxMemory());
    }

    public ServiceInfoSnapshot getFreeNonStartedService(String taskName)
    {
        return Iterables.first(CloudNet.getInstance().getCloudServiceManager().getGlobalServiceInfoSnapshots().values(), new Predicate<ServiceInfoSnapshot>() {
            @Override
            public boolean test(ServiceInfoSnapshot serviceInfoSnapshot)
            {
                return serviceInfoSnapshot.getServiceId().getTaskName().equalsIgnoreCase(taskName) &&
                    (serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.PREPARED || serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.DEFINED);
            }
        });
    }

    public ServiceInfoSnapshot createSmartCloudService(ServiceTask serviceTask, SmartServiceTaskConfig serviceTaskConfig)
    {
        List<ServiceTemplate> serviceTemplates = Iterables.newArrayList(serviceTask.getTemplates()), outTemplates = Iterables.newArrayList();

        if (!serviceTemplates.isEmpty())
            switch (serviceTaskConfig.getTemplateInstaller())
            {
                case INSTALL_ALL:
                {
                    outTemplates.addAll(serviceTemplates);
                }
                break;
                case INSTALL_RANDOM:
                {
                    Random random = new Random();

                    int size = random.nextInt(serviceTemplates.size());

                    for (int i = 0; i < size; ++i)
                    {
                        ServiceTemplate item = serviceTemplates.get(random.nextInt(serviceTemplates.size()));
                        serviceTemplates.remove(item);
                        outTemplates.add(item);
                    }
                }
                break;
                case INSTALL_RANDOM_ONCE:
                {
                    ServiceTemplate item = serviceTemplates.get(new Random().nextInt(serviceTemplates.size()));
                    serviceTemplates.remove(item);
                    outTemplates.add(item);
                }
                break;
            }

        int maxMemory = serviceTask.getProcessConfiguration().getMaxHeapMemorySize();

        if (serviceTaskConfig.isDynamicMemoryAllocation())
        {
            int percent = getPercentOfFreeMemory(serviceTask);

            if (percent > 50)
                maxMemory = maxMemory - ((percent * serviceTaskConfig.getDynamicMemoryAllocationRange()) / 100);
            else
                maxMemory = maxMemory + ((percent * serviceTaskConfig.getDynamicMemoryAllocationRange()) / 100);
        }

        ServiceInfoSnapshot serviceInfoSnapshot = getDriver().createCloudService(
            new ServiceTask(
                serviceTask.getIncludes(),
                outTemplates,
                serviceTask.getDeployments(),
                serviceTask.getName(),
                serviceTask.getRuntime(),
                true,
                serviceTask.isStaticServices(),
                serviceTask.getAssociatedNodes(),
                serviceTask.getGroups(),
                new ProcessConfiguration(
                    serviceTask.getProcessConfiguration().getEnvironment(),
                    maxMemory,
                    serviceTask.getProcessConfiguration().getJvmOptions()
                ),
                serviceTask.getStartPort(),
                1
            )
        );

        if (serviceInfoSnapshot != null)
        {
            if (serviceTaskConfig.isDirectTemplatesAndInclusionsSetup())
            {
                CloudNetDriver.getInstance().includeWaitingServiceTemplates(serviceInfoSnapshot.getServiceId().getUniqueId());
                CloudNetDriver.getInstance().includeWaitingServiceInclusions(serviceInfoSnapshot.getServiceId().getUniqueId());
            }

            providedSmartServices.put(serviceInfoSnapshot.getServiceId().getUniqueId(), new CloudNetServiceSmartProfile(
                serviceInfoSnapshot.getServiceId().getUniqueId(),
                new AtomicInteger(serviceTaskConfig.getAutoStopTimeByUnusedServiceInSeconds())
            ));
        }

        return serviceInfoSnapshot;
    }

    public void load()
    {
        reloadConfig();

        smartServiceTaskConfigurations.clear();
        smartServiceTaskConfigurations.addAll(getConfig().get("smartTasks", SMART_SERVICE_TASKS_CONFIGURATIONS,
            Collections.singletonList(
                new SmartServiceTaskConfig(
                    "Lobby",
                    10,
                    true,
                    0,
                    0,
                    true,
                    128,
                    0,
                    180,
                    false,
                    100,
                    300,
                    TemplateInstaller.INSTALL_ALL
                )
            )));

        saveConfig();
    }

    public void publishUpdateConfiguration(IPacketSender packetSender)
    {
        packetSender.sendPacket(new PacketClientServerChannelMessage(
            "cloudnet_smart_module",
            "update_configuration",
            new JsonDocument("smartServiceTaskConfiguration", getSmartServiceTaskConfigurations())
        ));
    }

    public void setSmartServiceTaskConfigurations(Collection<SmartServiceTaskConfig> configs)
    {
        smartServiceTaskConfigurations.clear();
        smartServiceTaskConfigurations.addAll(configs);

        getConfig().append("smartTasks", configs);
        saveConfig();
    }
}