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

package de.dytanic.cloudnet.ext.bridge.platform;

import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public abstract class PlatformPlayerExecutorAdapter implements PlayerExecutor {

  @Override
  public void sendTitle(@NonNull Title title) {
    // get the title times
    var times = title.times();
    if (times == null) {
      times = Title.DEFAULT_TIMES;
    }
    // send the title
    this.sendTitle(
      title.title(),
      title.subtitle(),
      (int) times.fadeIn().toMillis() / 50,
      (int) times.stay().toMillis() / 50,
      (int) times.fadeOut().toMillis() / 50);
  }

  protected void sendTitle(@NonNull Component title, @NonNull Component subtitle, int fadeIn, int stay, int fadeOut) {
    // no-op
  }
}
