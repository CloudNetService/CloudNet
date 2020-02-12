package eu.cloudnetservice.cloudnet.ext.npcs.configuration;


import java.util.*;

public class NPCConfiguration {

    public static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>();

    static {
        // TODO: default messages
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
