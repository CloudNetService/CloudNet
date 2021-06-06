package de.dytanic.cloudnet.service.defaults;

import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceFactory;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;

public class DefaultCloudServiceFactory implements ICloudServiceFactory {

  private final String runtime;
  private final BiFunction<ICloudServiceManager, ServiceConfiguration, ICloudService> factoryFunction;

  public DefaultCloudServiceFactory(String runtime,
    BiFunction<ICloudServiceManager, ServiceConfiguration, ICloudService> factoryFunction) {
    this.runtime = runtime;
    this.factoryFunction = factoryFunction;
  }

  @Override
  public @NotNull String getRuntime() {
    return this.runtime;
  }

  @Override
  public ICloudService createCloudService(@NotNull ICloudServiceManager cloudServiceManager,
    @NotNull ServiceConfiguration serviceConfiguration) {
    return this.factoryFunction.apply(cloudServiceManager, serviceConfiguration);
  }
}
