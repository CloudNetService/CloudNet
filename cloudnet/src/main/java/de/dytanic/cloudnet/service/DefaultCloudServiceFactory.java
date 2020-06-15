package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class DefaultCloudServiceFactory implements ICloudServiceFactory {

    private final String runtime;
    private final BiFunction<ICloudServiceManager, ServiceConfiguration, ICloudService> factoryFunction;

    public DefaultCloudServiceFactory(String runtime, BiFunction<ICloudServiceManager, ServiceConfiguration, ICloudService> factoryFunction) {
        this.runtime = runtime;
        this.factoryFunction = factoryFunction;
    }

    @Override
    public @NotNull String getRuntime() {
        return this.runtime;
    }

    @Override
    public ICloudService createCloudService(@NotNull ICloudServiceManager cloudServiceManager, @NotNull ServiceConfiguration serviceConfiguration) {
        return this.factoryFunction.apply(cloudServiceManager, serviceConfiguration);
    }
}
