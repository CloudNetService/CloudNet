package de.dytanic.cloudnet.provider.service;

import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public class EmptySpecificCloudServiceProvider implements SpecificCloudServiceProvider {

    public static final EmptySpecificCloudServiceProvider INSTANCE = new EmptySpecificCloudServiceProvider();

    private EmptySpecificCloudServiceProvider() {
    }

    @Override
    public @Nullable ServiceInfoSnapshot getServiceInfoSnapshot() {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public @Nullable ServiceInfoSnapshot forceUpdateServiceInfo() {
        return null;
    }

    @Override
    public void addServiceTemplate(@NotNull ServiceTemplate serviceTemplate) {
    }

    @Override
    public void addServiceRemoteInclusion(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
    }

    @Override
    public void addServiceDeployment(@NotNull ServiceDeployment serviceDeployment) {
    }

    @Override
    public Queue<String> getCachedLogMessages() {
        return new LinkedBlockingDeque<>();
    }

    @Override
    public void setCloudServiceLifeCycle(@NotNull ServiceLifeCycle lifeCycle) {
    }

    @Override
    public void restart() {
    }

    @Override
    public void kill() {
    }

    @Override
    public void runCommand(@NotNull String command) {
    }

    @Override
    public void includeWaitingServiceTemplates() {
    }

    @Override
    public void includeWaitingServiceInclusions() {
    }

    @Override
    public void deployResources(boolean removeDeployments) {
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> getServiceInfoSnapshotAsync() {
        return CompletedTask.create(null);
    }

    @Override
    public @NotNull ITask<Boolean> isValidAsync() {
        return CompletedTask.create(false);
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> forceUpdateServiceInfoAsync() {
        return CompletedTask.create(null);
    }

    @Override
    public @NotNull ITask<Void> addServiceTemplateAsync(@NotNull ServiceTemplate serviceTemplate) {
        return CompletedTask.voidTask();
    }

    @Override
    public @NotNull ITask<Void> addServiceRemoteInclusionAsync(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
        return CompletedTask.voidTask();
    }

    @Override
    public @NotNull ITask<Void> addServiceDeploymentAsync(@NotNull ServiceDeployment serviceDeployment) {
        return CompletedTask.voidTask();
    }

    @Override
    public @NotNull ITask<Queue<String>> getCachedLogMessagesAsync() {
        return CompletedTask.create(this.getCachedLogMessages());
    }

    @Override
    public @NotNull ITask<Void> setCloudServiceLifeCycleAsync(@NotNull ServiceLifeCycle lifeCycle) {
        return CompletedTask.voidTask();
    }

    @Override
    public @NotNull ITask<Void> restartAsync() {
        return CompletedTask.voidTask();
    }

    @Override
    public @NotNull ITask<Void> killAsync() {
        return CompletedTask.voidTask();
    }

    @Override
    public @NotNull ITask<Void> runCommandAsync(@NotNull String command) {
        return CompletedTask.voidTask();
    }

    @Override
    public @NotNull ITask<Void> includeWaitingServiceTemplatesAsync() {
        return CompletedTask.voidTask();
    }

    @Override
    public @NotNull ITask<Void> includeWaitingServiceInclusionsAsync() {
        return CompletedTask.voidTask();
    }

    @Override
    public @NotNull ITask<Void> deployResourcesAsync(boolean removeDeployments) {
        return CompletedTask.voidTask();
    }
}
