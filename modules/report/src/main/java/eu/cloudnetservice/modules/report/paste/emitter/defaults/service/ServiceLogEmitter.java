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

package eu.cloudnetservice.modules.report.paste.emitter.defaults.service;

import eu.cloudnetservice.cloudnet.node.service.CloudService;
import eu.cloudnetservice.modules.report.paste.emitter.ReportDataEmitter;
import org.jetbrains.annotations.NotNull;

public class ServiceLogEmitter implements ReportDataEmitter<CloudService> {

  @Override
  public void emitData(@NotNull StringBuilder builder, @NotNull CloudService context) {
    for (var cachedLogMessage : context.cachedLogMessages()) {
      builder.append(cachedLogMessage).append("\n");
    }
    builder.append("\n");
  }
}
