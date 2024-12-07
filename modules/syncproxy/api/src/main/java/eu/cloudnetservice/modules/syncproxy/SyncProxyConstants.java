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

package eu.cloudnetservice.modules.syncproxy;

public final class SyncProxyConstants {

  public static final String SYNC_PROXY_CHANNEL = "sync_proxy_internal";
  public static final String SYNC_PROXY_UPDATE_CONFIG = "update_syncproxy_config";
  public static final String SYNC_PROXY_CONFIG_REQUEST = "request_syncproxy_config";

  private SyncProxyConstants() {
    throw new UnsupportedOperationException();
  }
}
