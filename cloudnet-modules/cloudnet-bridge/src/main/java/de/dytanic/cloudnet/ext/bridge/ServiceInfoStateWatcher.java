package de.dytanic.cloudnet.ext.bridge;


import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ServiceInfoStateWatcher {

    protected final Map<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> services = new ConcurrentHashMap<>();

    public ServiceInfoStateWatcher() {
        // including the already existing services
        CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().stream()
                .filter(this::shouldWatchService)
                .forEach(serviceInfoSnapshot -> this.putService(serviceInfoSnapshot, this.fromServiceInfoSnapshot(serviceInfoSnapshot), false));
    }

    protected abstract void handleUpdate();

    protected abstract boolean shouldWatchService(ServiceInfoSnapshot serviceInfoSnapshot);

    protected abstract boolean shouldShowFullServices();

    private void putService(ServiceInfoSnapshot serviceInfoSnapshot, ServiceInfoState serviceInfoState) {
        this.putService(serviceInfoSnapshot, serviceInfoState, true);
    }

    private void putService(ServiceInfoSnapshot serviceInfoSnapshot, ServiceInfoState serviceInfoState, boolean fireUpdate) {
        if (!this.shouldWatchService(serviceInfoSnapshot)) {
            return;
        }

        this.services.put(serviceInfoSnapshot.getServiceId().getUniqueId(), new Pair<>(serviceInfoSnapshot, serviceInfoState));

        if (fireUpdate) {
            this.handleUpdate();
        }
    }

    @EventListener
    public void handle(CloudServiceRegisterEvent event) {
        this.putService(event.getServiceInfo(), ServiceInfoState.STOPPED);
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        this.putService(event.getServiceInfo(), ServiceInfoState.STARTING);
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event) {
        ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();
        this.putService(serviceInfoSnapshot, this.fromServiceInfoSnapshot(serviceInfoSnapshot));
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        this.putService(event.getServiceInfo(), ServiceInfoState.STOPPED);
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();
        this.putService(serviceInfoSnapshot, this.fromServiceInfoSnapshot(serviceInfoSnapshot));
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

        if (!this.shouldWatchService(serviceInfoSnapshot)) {
            return;
        }

        this.services.remove(serviceInfoSnapshot.getServiceId().getUniqueId());
        this.handleUpdate();
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        this.putService(event.getServiceInfo(), ServiceInfoState.STOPPED);
    }

    private ServiceInfoState fromServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot) {
        if (serviceInfoSnapshot.getLifeCycle() != ServiceLifeCycle.RUNNING || ServiceInfoSnapshotUtil.isIngameService(serviceInfoSnapshot)) {
            return ServiceInfoState.STOPPED;
        }

        if (ServiceInfoSnapshotUtil.isEmptyService(serviceInfoSnapshot)) {
            return ServiceInfoState.EMPTY_ONLINE;
        }

        if (ServiceInfoSnapshotUtil.isFullService(serviceInfoSnapshot)) {
            if (this.shouldShowFullServices()) {
                return ServiceInfoState.FULL_ONLINE;
            } else {
                return ServiceInfoState.STOPPED;
            }
        }

        if (ServiceInfoSnapshotUtil.isStartingService(serviceInfoSnapshot)) {
            return ServiceInfoState.STARTING;
        }

        if (serviceInfoSnapshot.isConnected() &&
                serviceInfoSnapshot.getProperties().getBoolean("Online")) {
            return ServiceInfoState.ONLINE;
        } else {
            return ServiceInfoState.STOPPED;
        }
    }

    public Map<UUID, Pair<ServiceInfoSnapshot, ServiceInfoState>> getServices() {
        return this.services;
    }

    public enum ServiceInfoState {
        STOPPED(0),
        STARTING(1),
        EMPTY_ONLINE(2),
        ONLINE(3),
        FULL_ONLINE(4);

        private final int value;

        ServiceInfoState(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

}
