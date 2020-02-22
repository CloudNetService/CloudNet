package eu.cloudnetservice.cloudnet.ext.npcs.configuration;


import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCConstants;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class NPCConfiguration {

    public static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>();

    static {
        DEFAULT_MESSAGES.put("command-create-invalid-uuid", "§7The provided UUID doesn't follow the correct format!");
        DEFAULT_MESSAGES.put("command-create-invalid-material", "§7The provided item isn't a valid material! (Use AIR for no item in hand)");
        DEFAULT_MESSAGES.put("command-create-texture-fetch-fail", "§7Unable to fetch skin of the provided UUID! Try again later.");
        DEFAULT_MESSAGES.put("command-create-display-name-too-long", "§7The NPC displayName cannot be longer than 16 chars!");
        DEFAULT_MESSAGES.put("command-create-success", "§7Successfully created the server selector NPC.");
        DEFAULT_MESSAGES.put("command-edit-info-line-success", "§7Successfully edited the info line of the NPC.");
        DEFAULT_MESSAGES.put("command-remove-success", "§7Successfully removed the server selector NPC.");
        DEFAULT_MESSAGES.put("command-no-npc-in-range", "§7There is no NPC in the range of 5 blocks!");
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

    private Collection<NPCConfigurationEntry> configurations = new ArrayList<>(Collections.singletonList(new NPCConfigurationEntry()));

    private Map<String, String> messages = DEFAULT_MESSAGES;

    public NPCConfiguration() {
    }

    public NPCConfiguration(Collection<NPCConfigurationEntry> configurations, Map<String, String> messages) {
        this.configurations = configurations;
        this.messages = messages;
    }

    public Collection<NPCConfigurationEntry> getConfigurations() {
        return configurations;
    }

    public Map<String, String> getMessages() {
        return messages;
    }

}
