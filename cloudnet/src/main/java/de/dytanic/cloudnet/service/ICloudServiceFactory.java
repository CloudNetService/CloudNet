package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.service.ServiceConfiguration;

public interface ICloudServiceFactory {

  ICloudService createCloudService(ICloudServiceManager cloudServiceManager,
    ServiceConfiguration serviceConfiguration);

}