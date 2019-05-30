package de.dytanic.cloudnet.event.service.task;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public final class ServiceTaskAddEvent extends Event implements ICancelable {

  private final ICloudServiceManager cloudServiceManager;

  private final ServiceTask task;

  @Setter
  private boolean cancelled;

}