package de.dytanic.cloudnet;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.service.ICloudService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Queue;
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
        int launchServicesTick = 0, clusterUpdateTick = 0;

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

                // TODO: request all node snapshots instead of every node sending them
                if (++clusterUpdateTick >= (TPS / 2)) {
                    this.cloudNet.publishNetworkClusterNodeInfoSnapshotUpdate();
                    clusterUpdateTick = 0;
                }

                if (++launchServicesTick >= TPS) {
                    this.startService();
                    launchServicesTick = 0;
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

                if (this.cloudNet.canStartServices(serviceTask) && serviceTask.getMinServiceCount() > runningServicesCount) {
                    // checking if there is a prepared service that can be started instead of creating a new service
                    if (this.startPreparedService(serviceTask.getName(), taskServices)) {
                        return;
                    }

                    if (this.cloudNet.competeWithCluster(serviceTask)) {
                        // this is the best node to create and start a new service
                        this.cloudNet.getCloudServiceManager().createCloudService(ServiceConfiguration.builder(serviceTask).build()).onComplete(cloudService -> {
                            if (cloudService != null) {
                                try {
                                    cloudService.start();
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }
        }
    }


    private boolean startPreparedService(String taskName, Collection<ServiceInfoSnapshot> taskServices) {
        Collection<String> preparedServiceNodeUniqueIds = taskServices.stream()
                .filter(taskService -> taskService.getLifeCycle() == ServiceLifeCycle.PREPARED)
                .map(taskService -> taskService.getServiceId().getNodeUniqueId())
                .collect(Collectors.toSet());

        boolean servicesPrepared = !preparedServiceNodeUniqueIds.isEmpty();

        if (servicesPrepared && this.cloudNet.competeWithCluster(preparedServiceNodeUniqueIds)) {
            // this is the best node to start one of the prepared services
            this.cloudNet.getCloudServiceManager().getLocalCloudServices(taskName)
                    .stream()
                    .filter(cloudService -> cloudService.getLifeCycle() == ServiceLifeCycle.PREPARED)
                    .findFirst()
                    .ifPresent(cloudService -> {
                        try {
                            cloudService.start();
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    });
        }

        return servicesPrepared;
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
