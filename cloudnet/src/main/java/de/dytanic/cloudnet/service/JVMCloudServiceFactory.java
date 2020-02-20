package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import org.jetbrains.annotations.NotNull;

final class JVMCloudServiceFactory implements ICloudServiceFactory {

    @Override
    public ICloudService createCloudService(@NotNull ICloudServiceManager cloudServiceManager, @NotNull ServiceConfiguration serviceConfiguration) {
        return new JVMCloudService(cloudServiceManager, serviceConfiguration);
    }
}