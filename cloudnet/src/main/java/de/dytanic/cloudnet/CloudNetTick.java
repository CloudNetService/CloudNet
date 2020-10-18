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
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CloudNetTick {

    public static final int TPS = CloudNet.TPS;

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

                if (launchServicesTick++ >= TPS) {
                    if (this.cloudNet.isHeadNode()) {
                        this.launchServices();
                    }

                    launchServicesTick = 0;
                }

                this.stopDeadServices();
                if (clusterUpdateTick++ >= TPS) {
                    this.cloudNet.publishNetworkClusterNodeInfoSnapshotUpdate();
                    clusterUpdateTick = 0;
                }

                this.updateServiceLogs();
                this.cloudNet.getEventManager().callEvent(new CloudNetTickEvent());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void launchServices() {
        for (ServiceTask serviceTask : this.cloudNet.getServiceTaskProvider().getPermanentServiceTasks()) {
            if (serviceTask.canStartServices()) {
                Collection<ServiceInfoSnapshot> taskServices = this.cloudNet.getCloudServiceProvider().getCloudServices(serviceTask.getName());
                long runningTaskServices = taskServices.stream().filter(taskService -> taskService.getLifeCycle() == ServiceLifeCycle.RUNNING).count();

                if (serviceTask.getMinServiceCount() > taskServices.size() || serviceTask.getMinServiceCount() > runningTaskServices) {
                    // there are still less running/prepared services of this task than the specified minServiceCount
                    // so looking for a service which isn't started yet and try to start it
                    Optional<ServiceInfoSnapshot> nonStartedServiceOptional = this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().values()
                            .stream()
                            .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getTaskName().equals(serviceTask.getName()))
                            .filter(cloudService -> cloudService.getLifeCycle() == ServiceLifeCycle.DEFINED || cloudService.getLifeCycle() == ServiceLifeCycle.PREPARED)
                            .findFirst();
                    if (nonStartedServiceOptional.isPresent()) {
                        this.tryStartService(nonStartedServiceOptional.get());
                    } else {
                        // There is no existing service to start and there are less services existing of this task
                        // than the specified minServiceCount, so starting a new service on the best node to do so
                        ServiceInfoSnapshot info = this.cloudNet.getCloudServiceFactory().createCloudService(ServiceConfiguration.builder(serviceTask).build());
                        if (info != null) {
                            this.tryStartService(info);
                        }
                    }
                }
            }
        }
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

    private void tryStartService(ServiceInfoSnapshot serviceInfoSnapshot) {
        if (serviceInfoSnapshot.getServiceId().getNodeUniqueId().equals(CloudNet.getInstance().getComponentName())) {
            ICloudService cloudService = this.cloudNet.getCloudServiceManager().getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());
            if (cloudService == null) {
                // We found a ghost service (a service which has no CloudService but a still a ServiceInfoSnapshot)
                this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().remove(serviceInfoSnapshot.getServiceId().getUniqueId());
                return;
            }

            try {
                cloudService.start();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        } else {
            serviceInfoSnapshot.provider().start();
        }
    }
}
