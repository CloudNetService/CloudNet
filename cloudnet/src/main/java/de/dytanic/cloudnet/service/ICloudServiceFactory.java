package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ICloudServiceFactory {

    @NotNull
    String getRuntime();

    @Nullable
    ICloudService createCloudService(@NotNull ICloudServiceManager cloudServiceManager, @NotNull ServiceConfiguration serviceConfiguration);

}