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

package de.dytanic.cloudnet.ext.syncproxy;

public final class SyncProxyConstants {

  public static final String SYNC_PROXY_CHANNEL_NAME = "sync_bungee_channel";
  public static final String SYNC_PROXY_UPDATE_CONFIGURATION = "update_sync_bungee_configuration";
  public static final String SYNC_PROXY_CHANNEL_GET_CONFIGURATION = "sync_bungee_get_sync_bungee_configuration";

  public static boolean CLOUD_PERMS_ENABLED;

  static {
    try {
      Class.forName("de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement");
      CLOUD_PERMS_ENABLED = true;
    } catch (ClassNotFoundException ignored) {
      CLOUD_PERMS_ENABLED = false;
    }
  }

  public SyncProxyConstants() {
    throw new UnsupportedOperationException();
  }

}
