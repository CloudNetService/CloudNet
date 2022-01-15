/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.report.paste.emitter.defaults.node;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.modules.report.paste.emitter.ReportDataEmitter;

public class NodeSnapshotEmitter implements ReportDataEmitter<NetworkClusterNodeInfoSnapshot> {

  @Override
  public void emitData(@NonNull StringBuilder builder, @NonNull NetworkClusterNodeInfoSnapshot context) {
    builder
      .append(" - NodeSnapshot ")
      .append(context.node().uniqueId())
      .append(" - \n")
      .append(JsonDocument.newDocument(context).toPrettyJson())
      .append("\n")
      .append(" - NodeSnapshot END - \n");
  }
}
