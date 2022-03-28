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

package eu.cloudnetservice.modules.syncproxy.config;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.syncproxy.SyncProxyConstants;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;
import lombok.NonNull;

public record SyncProxyTabList(@NonNull String header, @NonNull String footer) {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull SyncProxyTabList tabList) {
    return builder()
      .header(tabList.header())
      .footer(tabList.footer());
  }

  public static @NonNull String replaceTabListItem(
    @NonNull String input,
    @NonNull UUID playerUniqueId,
    int onlinePlayers,
    int maxPlayers
  ) {
    input = input
      .replace("%proxy%", Wrapper.instance().serviceId().name())
      .replace("%proxy_uniqueId%", Wrapper.instance().serviceId().uniqueId().toString())
      .replace("%proxy_task_name%", Wrapper.instance().serviceId().taskName())
      .replace("%time%", DATE_FORMAT.format(System.currentTimeMillis()))
      .replace("%online_players%", String.valueOf(onlinePlayers))
      .replace("%max_players%", String.valueOf(maxPlayers));

    if (SyncProxyConstants.CLOUD_PERMS_ENABLED) {
      var permissionManagement = CloudNetDriver.instance().permissionManagement();

      var permissionUser = permissionManagement.user(playerUniqueId);

      if (permissionUser != null) {
        var group = permissionManagement.highestPermissionGroup(permissionUser);

        if (group != null) {
          input = input.replace("%prefix%", group.prefix())
            .replace("%suffix%", group.suffix())
            .replace("%display%", group.display())
            .replace("%color%", group.color())
            .replace("%group%", group.name());
        }
      }
    }

    return input.replace("&", "§");
  }

  public static class Builder {

    private String header;
    private String footer;

    public @NonNull Builder header(@NonNull String header) {
      this.header = header;
      return this;
    }

    public @NonNull Builder footer(@NonNull String footer) {
      this.footer = footer;
      return this;
    }

    public @NonNull SyncProxyTabList build() {
      Preconditions.checkNotNull(this.header, "Missing header");
      Preconditions.checkNotNull(this.footer, "Missing footer");

      return new SyncProxyTabList(this.header, this.footer);
    }
  }
}
