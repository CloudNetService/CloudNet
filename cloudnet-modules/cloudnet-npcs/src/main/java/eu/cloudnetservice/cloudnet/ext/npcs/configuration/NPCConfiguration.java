package eu.cloudnetservice.cloudnet.ext.npcs.configuration;


import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCConstants;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class NPCConfiguration {

    public static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>();

    static {
        // TODO: default messages
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
