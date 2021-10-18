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

package eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.config.IConfiguration;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.ReportDataEmitter;

public class NodeConfigurationEmitter implements ReportDataEmitter<NetworkClusterNodeInfoSnapshot> {

  @Override
  public void emitData(StringBuilder builder, NetworkClusterNodeInfoSnapshot context) {
    builder
      .append(" - Node Configuration ")
      .append(context.getNode().getUniqueId())
      .append(" - \n");
    IConfiguration configuration = CloudNet.getInstance().getConfig();

    builder
      .append(JsonDocument.newDocument(configuration).toPrettyJson()).append("\n")
      .append(" - Node Configuration END - \n");
  }
}
