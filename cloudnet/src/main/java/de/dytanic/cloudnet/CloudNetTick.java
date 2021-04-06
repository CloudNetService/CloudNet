package de.dytanic.cloudnet;

import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.service.ICloudService;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class CloudNetTick {

    public static final int TPS = CloudNet.TPS;

    @NotNull
    private final Queue<ITask<?>> processQueue = new ConcurrentLinkedQueue<>();

    private final CloudNet cloudNet;

    public CloudNetTick(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    @NotNull
    public <T> ITask<T> runTask(Callable<T> runnable) {
        ITask<T> task = new ListenableTask<>(runnable);

        this.processQueue.offer(task);
        return task;
    }

    public void start() {
        long value = System.currentTimeMillis();
        long millis = 1000 / TPS;

        int launchServicesTick = 0;
        int clusterUpdateTick = 0;

        while (this.cloudNet.isRunning()) {
            try {
                long diff = System.currentTimeMillis() - value;
                if (diff < millis) {
                    try {
                        Thread.sleep(millis - diff);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }

                value = System.currentTimeMillis();

                while (!this.processQueue.isEmpty()) {
                    ITask<?> task = this.processQueue.poll();
                    if (task != null) {
                        task.call();
                    }
                }

                if (++clusterUpdateTick >= TPS) {
                    this.cloudNet.publishNetworkClusterNodeInfoSnapshotUpdate();
                    this.cloudNet.getClusterNodeServerProvider().checkForDeadNodes();
                    clusterUpdateTick = 0;
                }

                if (this.cloudNet.getClusterNodeServerProvider().getSelfNode().isHeadNode()) {
                    if (++launchServicesTick >= TPS * 2) {
                        this.startService();
                        launchServicesTick = 0;
                    }
                }

                this.stopDeadServices();
                this.updateServiceLogs();

                this.cloudNet.getEventManager().callEvent(new CloudNetTickEvent());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void startService() {
        for (ServiceTask serviceTask : this.cloudNet.getServiceTaskProvider().getPermanentServiceTasks()) {
            if (serviceTask.canStartServices()) {
                Collection<ServiceInfoSnapshot> taskServices = this.cloudNet.getCloudServiceProvider().getCloudServices(serviceTask.getName());

                long runningServicesCount = taskServices.stream()
                        .filter(taskService -> taskService.getLifeCycle() == ServiceLifeCycle.RUNNING)
                        .count();

                if (serviceTask.getMinServiceCount() > runningServicesCount) {
                    // checking if there is a prepared service that can be started instead of creating a new service
                    if (this.startPreparedService(taskServices)) {
                        return;
                    }
                    // start a new service if no service is available
                    NodeServer nodeServer = this.cloudNet.searchLogicNodeServer(serviceTask);
                    if (nodeServer != null) {
                        // found the best node server to start the service on
                        nodeServer.getCloudServiceFactory()
                                .createCloudServiceAsync(ServiceConfiguration.builder(serviceTask).build())
                                .onComplete(snapshot -> this.startPreparedService(nodeServer, snapshot));
                    }
                }
            }
        }
    }

    private boolean startPreparedService(Collection<ServiceInfoSnapshot> taskServices) {
        Map<String, Set<ServiceInfoSnapshot>> preparedServices = taskServices.stream()
                .filter(taskService -> taskService.getLifeCycle() == ServiceLifeCycle.PREPARED)
                .collect(Collectors.groupingBy(info -> info.getServiceId().getNodeUniqueId(), Collectors.toSet()));

        if (!preparedServices.isEmpty()) {
            Pair<NodeServer, Set<ServiceInfoSnapshot>> logicServices = this.cloudNet.searchLogicNodeServer(preparedServices);
            if (logicServices != null && !logicServices.getSecond().isEmpty()) {
                Set<ServiceInfoSnapshot> services = logicServices.getSecond();
                if (services.size() == 1) {
                    return this.startPreparedService(logicServices.getFirst(), services.iterator().next());
                } else {
                    ServiceInfoSnapshot snapshot = services.stream()
                            .min(Comparator.comparingInt(info -> info.getServiceId().getTaskServiceId()))
                            .orElse(null);
                    if (snapshot != null) {
                        return this.startPreparedService(logicServices.getFirst(), snapshot);
                    }
                }
            }
        }
        return false;
    }

    private boolean startPreparedService(NodeServer nodeServer, ServiceInfoSnapshot snapshot) {
        SpecificCloudServiceProvider provider = nodeServer.getCloudServiceProvider(snapshot);
        if (provider != null) {
            provider.start();
            return true;
        }
        return false;
    }

    private void stopDeadServices() {
        for (ICloudService cloudService : this.cloudNet.getCloudServiceManager().getCloudServices().values()) {
            if (!cloudService.isAlive()) {
                cloudService.stop();
            }
        }
    }

    private void updateServiceLogs() {
        for (ICloudService cloudService : this.cloudNet.getCloudServiceManager().getCloudServices().values()) {
            cloudService.getServiceConsoleLogCache().update();
        }
    }

}
