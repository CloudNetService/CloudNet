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

package de.dytanic.cloudnet.ext.cloudperms.nukkit;

import cn.nukkit.Player;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import lombok.NonNull;

public final class NukkitPermissionInjectionHelper {

  private static final MethodHandle SET_PERM_FIELD;

  static {
    try {
      // get the perm field
      var permField = Player.class.getDeclaredField("perm");
      permField.setAccessible(true);
      // un reflect the permissible field
      SET_PERM_FIELD = MethodHandles.lookup().unreflectSetter(permField);
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  public static void injectPermissible(@NonNull Player player, IPermissionManagement management) {
    try {
      SET_PERM_FIELD.invoke(player, new NukkitCloudPermissionsPermissible(player, management));
    } catch (Throwable throwable) {
      throw new RuntimeException("Unable to inject permissible", throwable);
    }
  }
}
