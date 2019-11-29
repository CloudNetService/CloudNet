package de.dytanic.cloudnet.ext.signs.configuration.entry;


import java.util.function.Function;

public enum SignConfigurationEntryType {
    BUKKIT(targetGroup -> SignConfigurationEntry.createDefault(targetGroup, "GOLD_BLOCK", "EMERALD_BLOCK", "BEDROCK", "REDSTONE_BLOCK")),
    NUKKIT(targetGroup -> SignConfigurationEntry.createDefault(targetGroup, "41", "133", "7", "152"));

    private Function<String, SignConfigurationEntry> entryFunction;

    SignConfigurationEntryType(Function<String, SignConfigurationEntry> entryFunction) {
        this.entryFunction = entryFunction;
    }

    public SignConfigurationEntry createEntry(String targetGroup) {
        return this.entryFunction.apply(targetGroup);
    }

}
