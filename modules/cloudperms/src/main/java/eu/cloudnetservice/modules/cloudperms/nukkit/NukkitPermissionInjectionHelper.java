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

package eu.cloudnetservice.modules.cloudperms.nukkit;

import cn.nukkit.Player;
import dev.derklaro.reflexion.FieldAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import lombok.NonNull;

public final class NukkitPermissionInjectionHelper {

  private static final FieldAccessor PERM_FIELD_ACCESSOR = Reflexion.on(Player.class).findField("perm").orElse(null);

  private NukkitPermissionInjectionHelper() {
    throw new UnsupportedOperationException();
  }

  public static void injectPermissible(
    @NonNull Player player,
    @NonNull WrapperConfiguration wrapperConfiguration,
    @NonNull PermissionManagement management
  ) {
    PERM_FIELD_ACCESSOR.setValue(
      player,
      new NukkitCloudPermissionsPermissible(player, wrapperConfiguration, management));
  }
}
