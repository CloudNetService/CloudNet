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

package de.dytanic.cloudnet.wrapper.network.chunk;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.chunk.ChunkedPacketSessionOpenEvent;
import de.dytanic.cloudnet.driver.network.chunk.defaults.DefaultFileChunkedPacketHandler;
import org.jetbrains.annotations.NotNull;

public final class TemplateStorageCallbackListener {

  @EventListener
  public void handle(@NotNull ChunkedPacketSessionOpenEvent event) {
    if (event.getSession().getTransferChannel().equals("request_template_file_result")) {
      event.setHandler(new DefaultFileChunkedPacketHandler(
        event.getSession(),
        null,
        FileUtils.TEMP_DIR.resolve(event.getSession().getSessionUniqueId().toString())));
    }
  }
}
