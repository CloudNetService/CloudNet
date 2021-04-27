package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public interface ICloudServiceManager {

    @NotNull
    @Deprecated
    File getTempDirectory();

    @NotNull
    @Deprecated
    File getPersistenceServicesDirectory();

    @NotNull
    Path getTempDirectoryPath();

    @NotNull
    Path getPersistentServicesDirectoryPath();

    @NotNull
    Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots();

    @NotNull
    Map<UUID, ICloudService> getCloudServices();

    @NotNull
    Collection<ICloudServiceFactory> getCloudServiceFactories();

    @NotNull
    Optional<ICloudServiceFactory> getCloudServiceFactory(@Nullable String runtime);

    @Deprecated
    @ApiStatus.ScheduledForRemoval
    ICloudService runTask(@NotNull ServiceConfiguration serviceConfiguration);

    @ApiStatus.Internal
    ITask<ICloudService> createCloudService(@NotNull ServiceConfiguration serviceConfiguration);

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

    //-

    int getCurrentUsedHeapMemory();

    int getCurrentReservedMemory();

}