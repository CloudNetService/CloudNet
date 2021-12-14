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

package eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.service;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.ReportDataEmitter;
import java.util.Collection;

public class ServiceOverviewEmitter implements ReportDataEmitter<ICloudService> {

  @Override
  public void emitData(StringBuilder builder, ICloudService service) {
    var services = CloudNet.getInstance().getCloudServiceProvider().getCloudServices();
    builder.append(" - Other Services - \n");
    builder.append("Total services: ").append(services.size()).append("\n");

    for (var snapshot : services) {
      builder
        .append("Name: ")
        .append(snapshot.getName())
        .append(" | Lifecycle: ")
        .append(snapshot.getLifeCycle())
        .append("\n");
    }

    builder.append(" - Other Services END - \n\n");
  }
}
