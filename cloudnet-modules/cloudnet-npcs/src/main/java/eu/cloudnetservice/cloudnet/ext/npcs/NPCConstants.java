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

package eu.cloudnetservice.cloudnet.ext.npcs;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Set;

public class NPCConstants {

  public static final Type NPC_COLLECTION_TYPE = new TypeToken<Set<CloudNPC>>() {
  }.getType();

  public static final String NPC_CHANNEL_NAME = "cloudnet_npc_channel";
  public static final String NPC_CHANNEL_ADD_NPC_MESSAGE = "add_npc";
  public static final String NPC_CHANNEL_REMOVE_NPC_MESSAGE = "remove_npc";
  public static final String NPC_CHANNEL_GET_NPCS_MESSAGE = "get_npcs";
  public static final String NPC_CHANNEL_GET_CONFIGURATION_MESSAGE = "get_npc_configuration";
  public static final String NPC_CHANNEL_UPDATE_CONFIGURATION_MESSAGE = "update_npc_configuration";

  private NPCConstants() {
    throw new UnsupportedOperationException();
  }


}
