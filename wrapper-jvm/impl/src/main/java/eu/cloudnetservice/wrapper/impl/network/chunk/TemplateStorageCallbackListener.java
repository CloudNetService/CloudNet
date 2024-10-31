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

package eu.cloudnetservice.wrapper.impl.network.chunk;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.impl.network.chunk.DefaultFileChunkedPacketHandler;
import eu.cloudnetservice.driver.network.chunk.event.ChunkedPacketSessionOpenEvent;
import eu.cloudnetservice.utils.base.io.FileUtil;
import lombok.NonNull;

public final class TemplateStorageCallbackListener {

  @EventListener
  public void handle(@NonNull ChunkedPacketSessionOpenEvent event) {
    if (event.session().transferChannel().equals("request_template_file_result")) {
      event.handler(new DefaultFileChunkedPacketHandler(
        event.session(),
        null,
        FileUtil.TEMP_DIR.resolve(event.session().sessionUniqueId().toString())));
    }
  }
}
