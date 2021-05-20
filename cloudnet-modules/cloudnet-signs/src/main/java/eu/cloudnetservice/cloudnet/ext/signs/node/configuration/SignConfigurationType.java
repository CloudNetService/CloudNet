package eu.cloudnetservice.cloudnet.ext.signs.node.configuration;

import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;

public enum SignConfigurationType {
    JAVA {
        public SignConfigurationEntry createEntry(String targetGroup) {
            return SignConfigurationEntry.createDefault(targetGroup, "GOLD_BLOCK", "EMERALD_BLOCK", "BEDROCK", "REDSTONE_BLOCK");
        }
    },
    BEDROCK {
        public SignConfigurationEntry createEntry(String targetGroup) {
            return SignConfigurationEntry.createDefault(targetGroup, "41", "133", "7", "152");
        }
    };

    public abstract SignConfigurationEntry createEntry(String targetGroup);
}
