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

package de.dytanic.cloudnet.driver.network.def;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class NetworkConstants {

  // rpc (reserved ids: 0 - 50)
  public static final int INTERNAL_RPC_COM_CHANNEL = 0;
  public static final int CHUNKED_PACKET_COM_CHANNEL = 1;

  // general (reserved ids: 51 - 100)
  public static final int INTERNAL_AUTHORIZATION_CHANNEL = 51;
  public static final int INTERNAL_WRAPPER_TO_NODE_INFO_CHANNEL = 52;
  public static final int INTERNAL_LOCAL_DATABASE_SYNC_CHANNEL = 53;
  public static final int INTERNAL_LOCAL_DATABASE_SET_DATA_CHANNEL = 54;
  public static final int INTERNAL_DEBUGGING_CHANNEL = 55;
  public static final int INTERNAL_DRIVER_API_CHANNEL = 56;
  public static final int INTERNAL_DATABASE_API_CHANNEL = 57;

  // cluster (reserved ids: 101 - 150)
  public static final int CLUSTER_SERVICE_INFO_LIST_CHANNEL = 101;
  public static final int CLUSTER_GROUP_CONFIG_LIST_CHANNEL = 102;
  public static final int CLUSTER_TASK_LIST_CHANNEL = 103;
  public static final int CLUSTER_PERMISSION_DATA_CHANNEL = 104;
  public static final int CLUSTER_TEMPLATE_DEPLOY_CHANNEL = 105;
  public static final int CLUSTER_TEMPLATE_STORAGE_SYNC_CHANNEL = 106;
  public static final int CLUSTER_TEMPLATE_STORAGE_CHUNK_SYNC_CHANNEL = 107;
  public static final int CLUSTER_NODE_INFO_CHANNEL = 108;

  // information exchange (reserved ids: 151 - 200)
  public static final int SERVICE_INFO_PUBLISH_CHANNEL = 151;
  public static final int PERMISSIONS_PUBLISH_CHANNEL = 152;
  public static final int CHANNEL_MESSAGING_CHANNEL = 153;

  // channel message channels
  public static final String WRAPPER_TO_NODE = "wrapper_node";
  public static final String NODE_TO_WRAPPER = "node_wrapper";
  public static final String NODE_TO_NODE = "node_node";

  private NetworkConstants() {
    throw new UnsupportedOperationException();
  }
}
