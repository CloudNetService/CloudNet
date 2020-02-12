package eu.cloudnetservice.cloudnet.ext.npcs;


import java.util.*;

public class NPCConfiguration {

    public static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>();

    static {
        // TODO: default messages
    }


    private Map<String, String> messages = DEFAULT_MESSAGES;

    private Collection<NPCConfigurationEntry> configurations = new ArrayList<>(Collections.singletonList(new NPCConfigurationEntry()));

    public NPCConfiguration() {
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    public Collection<NPCConfigurationEntry> getConfigurations() {
        return configurations;
    }

}
