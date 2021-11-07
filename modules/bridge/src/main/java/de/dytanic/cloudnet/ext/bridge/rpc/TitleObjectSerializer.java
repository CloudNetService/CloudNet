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

package de.dytanic.cloudnet.ext.bridge.rpc;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Type;
import java.time.Duration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TitleObjectSerializer implements ObjectSerializer<Title> {

  @Override
  public void write(
    @NotNull DataBuf.Mutable dataBuf,
    @NotNull Title object,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // extract the times
    Times times = object.times();
    if (times == null) {
      times = Title.DEFAULT_TIMES;
    }
    // write the times
    dataBuf.writeLong(times.fadeIn().toMillis());
    dataBuf.writeLong(times.stay().toMillis());
    dataBuf.writeLong(times.fadeOut().toMillis());
    // write the title and subtitle
    dataBuf.writeObject(object.title());
    dataBuf.writeObject(object.subtitle());
  }

  @Override
  public @Nullable Object read(
    @NotNull DataBuf source,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // read the times
    Times times = Times.of(
      Duration.ofMillis(source.readLong()),
      Duration.ofMillis(source.readLong()),
      Duration.ofMillis(source.readLong()));
    // read the title and subtitle
    TextComponent title = source.readObject(TextComponent.class);
    TextComponent subtitle = source.readObject(TextComponent.class);
    // create the title
    return Title.title(title, subtitle, times);
  }
}
