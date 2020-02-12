package eu.cloudnetservice.cloudnet.ext.npcs;


import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;

public class NPCConstants {

    public static final Type NPC_COLLECTION_TYPE = new TypeToken<Collection<CloudNPC>>() {
    }.getType();

    public static final String NPC_CHANNEL_NAME = "cloudnet_npc_channel";

    private NPCConstants() {
        throw new UnsupportedOperationException();
    }


}
