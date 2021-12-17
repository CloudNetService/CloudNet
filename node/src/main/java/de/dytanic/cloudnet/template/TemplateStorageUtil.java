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

package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.event.template.ServiceTemplateInstallEvent;
import java.io.IOException;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * An util class to prepare created templates with needed files
 */
public final class TemplateStorageUtil {

  private TemplateStorageUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull LocalTemplateStorage localTemplateStorage() {
    return (LocalTemplateStorage) CloudNet.getInstance().localTemplateStorage();
  }

  public static @NotNull Path localPathInTemplate(@NotNull ServiceTemplate serviceTemplate, @NotNull String path) {
    return localTemplateStorage().getTemplatePath(serviceTemplate).resolve(path).normalize();
  }

  public static boolean createAndPrepareTemplate(
    @NotNull ServiceTemplate template,
    @NotNull SpecificTemplateStorage storage,
    @NotNull ServiceEnvironmentType env
  ) throws IOException {
    return createAndPrepareTemplate(template, storage, env, true);
  }

  public static boolean createAndPrepareTemplate(
    @NotNull ServiceTemplate template,
    @NotNull SpecificTemplateStorage storage,
    @NotNull ServiceEnvironmentType env,
    boolean installDefaultFiles
  ) throws IOException {
    if (!storage.exists()) {
      storage.create();
      storage.createDirectory("plugins");

      // call the installation event if the default installation process should be executed
      if (installDefaultFiles) {
        CloudNet.getInstance().eventManager().callEvent(new ServiceTemplateInstallEvent(template, storage, env));
      }

      return true;
    } else {
      return false;
    }
  }
}
