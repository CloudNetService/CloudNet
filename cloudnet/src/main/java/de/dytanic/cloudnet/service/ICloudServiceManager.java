package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public interface ICloudServiceManager {

    @NotNull
    File getTempDirectory();

    @NotNull
    File getPersistenceServicesDirectory();

    @NotNull
    Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots();

    @NotNull
    Map<UUID, ICloudService> getCloudServices();

    @NotNull
    Collection<ICloudServiceFactory> getCloudServiceFactories();

    @NotNull
    Optional<ICloudServiceFactory> getCloudServiceFactory(@Nullable String runtime);

    @Nullable
    @Deprecated
    @ApiStatus.Internal
    @ApiStatus.ScheduledForRemoval
    ICloudService runTask(@NotNull ServiceConfiguration serviceConfiguration);

    @NotNull
    @ApiStatus.Internal
    ITask<ServiceInfoSnapshot> buildService(@NotNull ServiceConfiguration serviceConfiguration);

    @ApiStatus.Internal
    void removeService(@NotNull UUID uniqueId);

    void startAllCloudServices();

    void stopAllCloudServices();

    void deleteAllCloudServices();

    //-

    @Nullable
    ICloudService getCloudService(@NotNull UUID uniqueId);

    @Nullable
    ICloudService getCloudService(@NotNull Predicate<ICloudService> predicate);

    /**
     * @deprecated moved to {@link #getLocalCloudServices(String)}
     */
    @Deprecated
    default Collection<ICloudService> getCloudServices(String taskName) {
        return this.getLocalCloudServices(taskName);
    }

    /**
     * @deprecated moved to {@link #getLocalCloudServices(Predicate)}
     */
    @Deprecated
    default Collection<ICloudService> getCloudServices(Predicate<ICloudService> predicate) {
        return this.getLocalCloudServices(predicate);
    }

    /**
     * @deprecated moved to {@link #getLocalCloudServices()}
     */
    @Deprecated
    default Collection<ICloudService> getServices() {
        return this.getLocalCloudServices();
    }

    Collection<ICloudService> getLocalCloudServices(@NotNull String taskName);

    Collection<ICloudService> getLocalCloudServices(@NotNull Predicate<ICloudService> predicate);

    Collection<ICloudService> getLocalCloudServices();

    Collection<Integer> getReservedTaskIds(@NotNull String task);

    Collection<UUID> getReservedTaskUniqueIds(@NotNull String task);
    //-

    int getCurrentUsedHeapMemory();

    int getCurrentReservedMemory();

}