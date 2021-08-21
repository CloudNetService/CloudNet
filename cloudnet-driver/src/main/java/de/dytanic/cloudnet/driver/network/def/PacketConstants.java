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

import java.util.concurrent.atomic.AtomicInteger;

public final class PacketConstants {

  private static final AtomicInteger CHANNEL_ID_COUNTER = new AtomicInteger();

  // rpc
  public static final int INTERNAL_RPC_COM_CHANNEL = CHANNEL_ID_COUNTER.getAndIncrement();
  public static final int CHUNKED_PACKET_COM_CHANNEL = CHANNEL_ID_COUNTER.getAndIncrement();

  // TODO: find out out which of these we still need

  // general
  public static final int INTERNAL_AUTHORIZATION_CHANNEL = 1;
  public static final int INTERNAL_WRAPPER_TO_NODE_INFO_CHANNEL = 2;
  public static final int INTERNAL_LOCAL_DATABASE_SYNC_CHANNEL = 3;
  public static final int INTERNAL_LOCAL_DATABASE_SET_DATA_CHANNEL = 4;
  public static final int INTERNAL_DEBUGGING_CHANNEL = 5;
  public static final int INTERNAL_DRIVER_API_CHANNEL = 6;
  public static final int INTERNAL_DATABASE_API_CHANNEL = 7;

  // cluster
  public static final int CLUSTER_SERVICE_INFO_LIST_CHANNEL = 8;
  public static final int CLUSTER_GROUP_CONFIG_LIST_CHANNEL = 9;
  public static final int CLUSTER_TASK_LIST_CHANNEL = 10;
  public static final int CLUSTER_PERMISSION_DATA_CHANNEL = 11;
  public static final int CLUSTER_TEMPLATE_DEPLOY_CHANNEL = 12;
  public static final int CLUSTER_TEMPLATE_STORAGE_SYNC_CHANNEL = 13;
  public static final int CLUSTER_TEMPLATE_STORAGE_CHUNK_SYNC_CHANNEL = 14;
  public static final int CLUSTER_NODE_INFO_CHANNEL = 15;

  // events
  public static final int SERVICE_INFO_PUBLISH_CHANNEL = 16;
  public static final int PERMISSIONS_PUBLISH_CHANNEL = 17;
  public static final int CHANNEL_MESSAGING_CHANNEL = 18;

  private PacketConstants() {
    throw new UnsupportedOperationException();
  }
}
