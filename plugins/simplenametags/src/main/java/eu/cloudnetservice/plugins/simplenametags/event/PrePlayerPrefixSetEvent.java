/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.plugins.simplenametags.event;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import lombok.NonNull;

public class PrePlayerPrefixSetEvent<P> extends Event {

  private final P player;
  private volatile PermissionGroup group;

  public PrePlayerPrefixSetEvent(@NonNull P player, @NonNull PermissionGroup group) {
    this.player = player;
    this.group = group;
  }

  public @NonNull P player() {
    return this.player;
  }

  public @NonNull PermissionGroup group() {
    return this.group;
  }

  public void group(@NonNull PermissionGroup group) {
    this.group = group;
  }
}
