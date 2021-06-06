package eu.cloudnetservice.cloudnet.ext.npcs;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Set;

public class NPCConstants {

  public static final Type NPC_COLLECTION_TYPE = new TypeToken<Set<CloudNPC>>() {
  }.getType();

  public static final String NPC_CHANNEL_NAME = "cloudnet_npc_channel";
  public static final String
    NPC_CHANNEL_ADD_NPC_MESSAGE = "add_npc",
    NPC_CHANNEL_REMOVE_NPC_MESSAGE = "remove_npc",
    NPC_CHANNEL_GET_NPCS_MESSAGE = "get_npcs",
    NPC_CHANNEL_GET_CONFIGURATION_MESSAGE = "get_npc_configuration",
    NPC_CHANNEL_UPDATE_CONFIGURATION_MESSAGE = "update_npc_configuration";

  private NPCConstants() {
    throw new UnsupportedOperationException();
  }


}
