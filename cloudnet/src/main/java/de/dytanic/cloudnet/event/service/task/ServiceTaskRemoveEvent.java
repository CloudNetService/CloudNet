package de.dytanic.cloudnet.event.service.task;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class ServiceTaskRemoveEvent extends Event {

  private final ICloudServiceManager cloudServiceManager;

  private final ServiceTask task;

  @Setter
  private boolean cancelled;
}