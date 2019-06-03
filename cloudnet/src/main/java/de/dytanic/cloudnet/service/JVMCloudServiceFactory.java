package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.service.ServiceConfiguration;

final class JVMCloudServiceFactory implements ICloudServiceFactory {

  @Override
  public ICloudService createCloudService(
      ICloudServiceManager cloudServiceManager,
      ServiceConfiguration serviceConfiguration) {
    return new JVMCloudService(cloudServiceManager, serviceConfiguration);
  }
}