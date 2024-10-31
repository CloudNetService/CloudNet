/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.template;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.node.event.template.ServiceTemplateInstallEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import lombok.NonNull;

/**
 * An util class to prepare created templates with needed files
 */
@Singleton
public final class TemplateStorageUtil {

  private final EventManager eventManager;

  @Inject
  public TemplateStorageUtil(@NonNull EventManager eventManager) {
    this.eventManager = eventManager;
  }

  public boolean createAndPrepareTemplate(
    @NonNull ServiceTemplate template,
    @NonNull TemplateStorage storage,
    @NonNull ServiceEnvironmentType env
  ) throws IOException {
    return this.createAndPrepareTemplate(template, storage, env, true);
  }

  public boolean createAndPrepareTemplate(
    @NonNull ServiceTemplate template,
    @NonNull TemplateStorage storage,
    @NonNull ServiceEnvironmentType env,
    boolean installDefaultFiles
  ) throws IOException {
    if (!storage.contains(template)) {
      storage.create(template);
      storage.createDirectory(template, "plugins");

      // call the installation event if the default installation process should be executed
      if (installDefaultFiles) {
        this.eventManager.callEvent(new ServiceTemplateInstallEvent(template, storage, env));
      }

      return true;
    } else {
      return false;
    }
  }
}
