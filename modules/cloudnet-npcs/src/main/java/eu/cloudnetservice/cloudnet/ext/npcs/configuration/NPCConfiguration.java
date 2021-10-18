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

package eu.cloudnetservice.cloudnet.ext.npcs.configuration;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class NPCConfiguration {

  public static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>();
  public static final NPCConfiguration EMPTY_CONFIGURATION = new NPCConfiguration();

  static {
    DEFAULT_MESSAGES.put("command-create-invalid-material",
      "§7The provided item isn't a valid material! (Use AIR for no item in hand)");
    DEFAULT_MESSAGES.put("command-create-texture-fetch-fail",
      "§7Unable to fetch the skin of the provided Minecraft name! Try again later.");
    DEFAULT_MESSAGES
      .put("command-create-display-name-too-long", "§7The NPC displayName cannot be longer than 16 chars!");
    DEFAULT_MESSAGES.put("command-create-success", "§7Successfully created the server selector NPC.");
    DEFAULT_MESSAGES.put("command-edit-invalid-action", "§7The provided action isn't valid!");
    DEFAULT_MESSAGES.put("command-edit-success", "§7Successfully edited the NPC.");
    DEFAULT_MESSAGES.put("command-remove-success", "§7Successfully removed the server selector NPC.");
    DEFAULT_MESSAGES.put("command-no-npc-in-range", "§7There is no NPC in the range of 5 blocks!");
    DEFAULT_MESSAGES.put("command-cleanup-success", "§7All NPCs on unloaded worlds have been removed successfully.");
  }

  private Collection<NPCConfigurationEntry> configurations = new ArrayList<>(
    Collections.singletonList(new NPCConfigurationEntry()));
  private Map<String, String> messages = DEFAULT_MESSAGES;

  public NPCConfiguration() {
  }

  public NPCConfiguration(Collection<NPCConfigurationEntry> configurations, Map<String, String> messages) {
    this.configurations = configurations;
    this.messages = messages;
  }

  /**
   * Updates the NPCConfiguration in the whole cluster
   *
   * @param npcConfiguration the new NPCConfiguration
   */
  public static void sendNPCConfigurationUpdate(@NotNull NPCConfiguration npcConfiguration) {
    CloudNetDriver.getInstance().getMessenger()
      .sendChannelMessage(
        NPCConstants.NPC_CHANNEL_NAME,
        NPCConstants.NPC_CHANNEL_UPDATE_CONFIGURATION_MESSAGE,
        new JsonDocument("npcConfiguration", npcConfiguration)
      );
  }

  public Collection<NPCConfigurationEntry> getConfigurations() {
    return this.configurations;
  }

  public Map<String, String> getMessages() {
    return this.messages;
  }

}
