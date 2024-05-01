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

package eu.cloudnetservice.modules.bridge.rpc;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.object.ObjectSerializer;
import eu.cloudnetservice.ext.component.ComponentFormats;
import java.lang.reflect.Type;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

/**
 * A serializer to write adventure components to a data buf.
 *
 * @since 4.0
 */
public final class ComponentObjectSerializer implements ObjectSerializer<Component> {

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull Component object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    dataBuf.writeString(ComponentFormats.JSON.fromAdventure(object));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Object read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    return ComponentFormats.JSON.toAdventure(source.readString());
  }
}
