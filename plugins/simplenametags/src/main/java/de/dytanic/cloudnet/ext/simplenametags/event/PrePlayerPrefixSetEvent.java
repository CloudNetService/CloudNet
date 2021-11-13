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

package de.dytanic.cloudnet.ext.simplenametags.event;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import org.jetbrains.annotations.NotNull;

public class PrePlayerPrefixSetEvent<P> extends Event {

  private final P player;
  private volatile PermissionGroup group;

  public PrePlayerPrefixSetEvent(@NotNull P player, @NotNull PermissionGroup group) {
    this.player = player;
    this.group = group;
  }

  public @NotNull P getPlayer() {
    return this.player;
  }

  public @NotNull PermissionGroup getGroup() {
    return this.group;
  }

  public void setGroup(@NotNull PermissionGroup group) {
    this.group = group;
  }
}
