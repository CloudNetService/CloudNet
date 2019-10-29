package de.dytanic.cloudnet.ext.smart;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.ext.smart.listener.CloudNetTickListener;
import de.dytanic.cloudnet.ext.smart.listener.CloudServiceListener;
import de.dytanic.cloudnet.ext.smart.listener.TaskDefaultSmartConfigListener;
import de.dytanic.cloudnet.ext.smart.util.SmartServiceTaskConfig;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

import java.util.*;

public final class CloudNetSmartModule extends NodeCloudNetModule {

    public static final String SMART_CONFIG_ENTRY = "smartConfig";

    private static CloudNetSmartModule instance;

    private final Map<UUID, CloudNetServiceSmartProfile> providedSmartServices = Maps.newConcurrentHashMap();

    public CloudNetSmartModule() {
        instance = this;
    }

    public static CloudNetSmartModule getInstance() {
        return CloudNetSmartModule.instance;
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
    public void initTaskConfigs() {
        Map<String, SmartServiceTaskConfig> oldSmartTasks = new HashMap<>();
        if (getModuleWrapper().getDataFolder().exists() && getConfig().contains("smartTasks")) {
            Collection<JsonDocument> smartTasks = getConfig().get("smartTasks", new TypeToken<Collection<JsonDocument>>() {
            }.getType());
            for (JsonDocument smartTaskJson : smartTasks) {
                SmartServiceTaskConfig smartTask = smartTaskJson.toInstanceOf(SmartServiceTaskConfig.class);
                smartTask.setEnabled(true);
                oldSmartTasks.put(
                        smartTaskJson.getString("task"),
                        smartTask
                );
            }
            FileUtils.delete(getModuleWrapper().getDataFolder());
        }

        for (ServiceTask task : this.getCloudNet().getPermanentServiceTasks()) {
            SmartServiceTaskConfig config = task.getProperties().get(SMART_CONFIG_ENTRY, SmartServiceTaskConfig.class);
            task.getProperties().append(
                    SMART_CONFIG_ENTRY,
                    oldSmartTasks.containsKey(task.getName()) ?
                            oldSmartTasks.get(task.getName()) :
                            config != null ? config : new SmartServiceTaskConfig()
            );
            this.getCloudNet().getCloudServiceManager().updatePermanentServiceTask(task);
        }
    }

    @ModuleTask(order = 16, event = ModuleLifeCycle.STARTED)
    public void initListeners() {
        registerListeners(new CloudNetTickListener(), new CloudServiceListener(), new TaskDefaultSmartConfigListener());
    }

    public SmartServiceTaskConfig getSmartServiceTaskConfig(ServiceTask task) {
        return task.getProperties().get(SMART_CONFIG_ENTRY, SmartServiceTaskConfig.class);
    }

    public SmartServiceTaskConfig getSmartServiceTaskConfig(ServiceInfoSnapshot serviceInfoSnapshot) {
        ServiceTask task = this.getCloudNet().getServiceTask(serviceInfoSnapshot.getServiceId().getTaskName());
        return task != null ? this.getSmartServiceTaskConfig(task) : null;
    }

    public boolean hasSmartServiceTaskConfig(ServiceTask task) {
        return task.getProperties().contains(SMART_CONFIG_ENTRY) && this.getSmartServiceTaskConfig(task).isEnabled();
    }

    public int getPercentOfFreeMemory(String nodeId, ServiceTask serviceTask) {
        NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot = CloudNet.getInstance().getNodeInfoSnapshot(nodeId);

        if (networkClusterNodeInfoSnapshot != null && !networkClusterNodeInfoSnapshot.getNode()
                .getUniqueId().equalsIgnoreCase(getCloudNetConfig().getIdentity().getUniqueId())) {
            int memory = (networkClusterNodeInfoSnapshot.getMaxMemory() - networkClusterNodeInfoSnapshot.getUsedMemory()) * 100;
            return networkClusterNodeInfoSnapshot.getMaxMemory() > 0 ? memory / networkClusterNodeInfoSnapshot.getMaxMemory() : memory;
        } else {
            int memory = (getCloudNet().getConfig().getMaxMemory() - getCloudNet().getCloudServiceManager().getCurrentUsedHeapMemory()) * 100;
            return getCloudNet().getConfig().getMaxMemory() > 0 ? memory / getCloudNet().getConfig().getMaxMemory() : memory;
        }
    }

    public ServiceInfoSnapshot getFreeNonStartedService(String taskName) {
        return Iterables.first(CloudNet.getInstance().getCloudServiceManager().getGlobalServiceInfoSnapshots().values(), serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getTaskName().equalsIgnoreCase(taskName) &&
                (serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.PREPARED || serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.DEFINED));
    }

    public void updateAsSmartService(ServiceConfiguration configuration, ServiceTask serviceTask, SmartServiceTaskConfig smartTask) {
        configuration.setTemplates(this.useSmartConfig(configuration.getTemplates(), smartTask));
        configuration.getProcessConfig().setMaxHeapMemorySize(
                this.useSmartConfig(serviceTask.getProcessConfiguration().getMaxHeapMemorySize(), configuration, serviceTask, smartTask)
        );
    }

    private ServiceTemplate[] useSmartConfig(ServiceTemplate[] rawTemplates, SmartServiceTaskConfig smartTask) {
        List<ServiceTemplate> templates = Arrays.asList(rawTemplates);
        List<ServiceTemplate> outTemplates = new ArrayList<>();
        switch (smartTask.getTemplateInstaller()) {
            case INSTALL_ALL: {
                outTemplates.addAll(templates);
                break;
            }
            case INSTALL_RANDOM: {
                if (!templates.isEmpty()) {
                    Random random = new Random();

                    int size = random.nextInt(templates.size());

                    for (int i = 0; i < size; ++i) {
                        ServiceTemplate item = templates.get(random.nextInt(templates.size()));
                        templates.remove(item);
                        outTemplates.add(item);
                    }
                }
                break;
            }
            case INSTALL_RANDOM_ONCE: {
                if (!templates.isEmpty()) {
                    ServiceTemplate item = templates.get(new Random().nextInt(templates.size()));
                    outTemplates.add(item);
                }
                break;
            }
        }
        return outTemplates.toArray(new ServiceTemplate[0]);
    }

    private int useSmartConfig(int maxMemory, ServiceConfiguration serviceConfiguration, ServiceTask serviceTask, SmartServiceTaskConfig smartTask) {
        if (smartTask.isDynamicMemoryAllocation()) {
            int percent = getPercentOfFreeMemory(serviceConfiguration.getServiceId().getNodeUniqueId(), serviceTask);

            if (percent > 50) {
                maxMemory = maxMemory - ((percent * smartTask.getDynamicMemoryAllocationRange()) / 100);
            } else {
                maxMemory = maxMemory + ((percent * smartTask.getDynamicMemoryAllocationRange()) / 100);
            }
        }
        return maxMemory;
    }


    public Map<UUID, CloudNetServiceSmartProfile> getProvidedSmartServices() {
        return this.providedSmartServices;
    }

}
