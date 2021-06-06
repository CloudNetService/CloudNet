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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class CloudNetTick {

    public static final int TPS = CloudNet.TPS;
    public static final int MILLIS_BETWEEN_TICKS = 1000 / TPS;

    private final CloudNet cloudNet;
    private final Queue<ITask<?>> processQueue = new ConcurrentLinkedQueue<>();

    public CloudNetTick(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    @NotNull
    public <T> ITask<T> runTask(@NotNull Callable<T> callable) {
        ITask<T> task = new ListenableTask<>(callable);
        this.processQueue.offer(task);
        return task;
    }

    public void start() {
        long lastTickLength;
        long currentTickNumber = 0;
        long lastTick = System.currentTimeMillis();

        while (this.cloudNet.isRunning()) {
            try {
                currentTickNumber++;

                lastTickLength = System.currentTimeMillis() - lastTick;
                if (lastTickLength < MILLIS_BETWEEN_TICKS) {
                    try {
                        Thread.sleep(MILLIS_BETWEEN_TICKS - lastTickLength);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }

                lastTick = System.currentTimeMillis();
                while (!this.processQueue.isEmpty()) {
                    ITask<?> task = this.processQueue.poll();
                    if (task != null) {
                        task.call();
                    }
                }

                if (this.cloudNet.getClusterNodeServerProvider().getSelfNode().isHeadNode()) {
                    if (currentTickNumber % TPS * 2 == 0) {
                        this.startService();
                    }
                }

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

    private boolean startPreparedService(@NotNull Collection<ServiceInfoSnapshot> taskServices) {
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

    private boolean startPreparedService(@NotNull NodeServer nodeServer, @Nullable ServiceInfoSnapshot snapshot) {
        if (snapshot != null) {
            SpecificCloudServiceProvider provider = nodeServer.getCloudServiceProvider(snapshot);
            if (provider != null) {
                provider.start();
                return true;
            }
        }
        return false;
    }
}
