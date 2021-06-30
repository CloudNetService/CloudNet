/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.service.handler;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.event.service.CloudServicePostDeleteEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostPrepareEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostStartEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostStartPrepareEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostStopEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreDeleteEvent;
import de.dytanic.cloudnet.event.service.CloudServicePrePrepareEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreStartPrepareEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreStopEvent;
import de.dytanic.cloudnet.service.ICloudService;

public class DefaultCloudServiceHandler implements CloudServiceHandler {

  public static final CloudServiceHandler INSTANCE = new DefaultCloudServiceHandler();

  @Override
  public boolean handlePreDelete(ICloudService service) {
    if (CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreDeleteEvent(service))
      .isCancelled()) {
      return false;
    }

    System.out.println(LanguageManager.getMessage("cloud-service-pre-delete-message")
      .replace("%task%", service.getServiceId().getTaskName())
      .replace("%serviceId%", String.valueOf(service.getServiceId().getTaskServiceId()))
      .replace("%id%", service.getServiceId().getUniqueId().toString())
    );
    return true;
  }

  @Override
  public void handlePostDelete(ICloudService service) {
    CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostDeleteEvent(service));
    CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-post-delete-message")
      .replace("%task%", service.getServiceId().getTaskName())
      .replace("%serviceId%", String.valueOf(service.getServiceId().getTaskServiceId()))
      .replace("%id%", service.getServiceId().getUniqueId().toString())
    );
  }

  @Override
  public boolean handlePrePrepare(ICloudService service) {
    if (CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePrePrepareEvent(service))
      .isCancelled()) {
      return false;
    }

    System.out.println(LanguageManager.getMessage("cloud-service-pre-prepared-message")
      .replace("%task%", service.getServiceId().getTaskName())
      .replace("%serviceId%", String.valueOf(service.getServiceId().getTaskServiceId()))
      .replace("%id%", service.getServiceId().getUniqueId().toString())
    );
    return true;
  }

  @Override
  public void handlePostPrepare(ICloudService service) {
    CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostPrepareEvent(service));
    CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-post-prepared-message")
      .replace("%serviceId%", String.valueOf(service.getServiceId().getTaskServiceId()))
      .replace("%task%", service.getServiceId().getTaskName())
      .replace("%id%", service.getServiceId().getUniqueId().toString())
    );
  }

  @Override
  public boolean handlePrePrepareStart(ICloudService service) {
    if (CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreStartPrepareEvent(service))
      .isCancelled()) {
      return false;
    }

    CloudNetDriver.getInstance().getLogger()
      .extended(LanguageManager.getMessage("cloud-service-pre-start-prepared-message")
        .replace("%task%", service.getServiceId().getTaskName())
        .replace("%serviceId%", String.valueOf(service.getServiceId().getTaskServiceId()))
        .replace("%id%", service.getServiceId().getUniqueId().toString())
      );
    return true;
  }

  @Override
  public void handlePostPrepareStart(ICloudService service) {
    CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostStartPrepareEvent(service));
    CloudNetDriver.getInstance().getLogger()
      .extended(LanguageManager.getMessage("cloud-service-post-start-prepared-message")
        .replace("%task%", service.getServiceId().getTaskName())
        .replace("%serviceId%", String.valueOf(service.getServiceId().getTaskServiceId()))
        .replace("%id%", service.getServiceId().getUniqueId().toString())
      );
  }

  @Override
  public void handlePreStart(ICloudService service) {
    System.out.println(LanguageManager.getMessage("cloud-service-pre-start-message")
      .replace("%task%", service.getServiceId().getTaskName())
      .replace("%serviceId%", String.valueOf(service.getServiceId().getTaskServiceId()))
      .replace("%id%", service.getServiceId().getUniqueId().toString())
    );
    CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreStartEvent(service));
  }

  @Override
  public void handlePostStart(ICloudService service) {
    CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostStartEvent(service));
    CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-post-start-message")
      .replace("%serviceId%", String.valueOf(service.getServiceId().getTaskServiceId()))
      .replace("%task%", service.getServiceId().getTaskName())
      .replace("%id%", service.getServiceId().getUniqueId().toString())
    );
  }

  @Override
  public boolean handlePreStop(ICloudService service) {
    if (CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreStopEvent(service)).isCancelled()) {
      return false;
    }

    System.out.println(LanguageManager.getMessage("cloud-service-pre-stop-message")
      .replace("%task%", service.getServiceId().getTaskName())
      .replace("%serviceId%", String.valueOf(service.getServiceId().getTaskServiceId()))
      .replace("%id%", service.getServiceId().getUniqueId().toString())
    );
    return true;
  }

  @Override
  public void handlePostStop(ICloudService service, int exitValue) {
    CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostStopEvent(service, exitValue));
    CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-post-stop-message")
      .replace("%task%", service.getServiceId().getTaskName())
      .replace("%serviceId%", String.valueOf(service.getServiceId().getTaskServiceId()))
      .replace("%id%", service.getServiceId().getUniqueId().toString())
      .replace("%exit_value%", String.valueOf(exitValue))
    );
  }
}
