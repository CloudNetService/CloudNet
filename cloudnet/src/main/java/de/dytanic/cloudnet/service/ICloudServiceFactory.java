package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import org.jetbrains.annotations.NotNull;

public interface ICloudServiceFactory {

    ICloudService createCloudService(@NotNull ICloudServiceManager cloudServiceManager, @NotNull ServiceConfiguration serviceConfiguration);

}