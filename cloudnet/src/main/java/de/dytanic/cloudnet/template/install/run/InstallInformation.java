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

package de.dytanic.cloudnet.template.install.run;

import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;

public class InstallInformation {

  private final ServiceVersionType serviceVersionType;
  private final ServiceVersion serviceVersion;
  private final TemplateStorage templateStorage;
  private final ServiceTemplate serviceTemplate;

  public InstallInformation(ServiceVersionType serviceVersionType, ServiceVersion serviceVersion,
    TemplateStorage templateStorage, ServiceTemplate serviceTemplate) {
    this.serviceVersionType = serviceVersionType;
    this.serviceVersion = serviceVersion;
    this.templateStorage = templateStorage;
    this.serviceTemplate = serviceTemplate;
  }

  public ServiceVersionType getServiceVersionType() {
    return this.serviceVersionType;
  }

  public ServiceVersion getServiceVersion() {
    return this.serviceVersion;
  }

  public TemplateStorage getTemplateStorage() {
    return this.templateStorage;
  }

  public ServiceTemplate getServiceTemplate() {
    return this.serviceTemplate;
  }

}
