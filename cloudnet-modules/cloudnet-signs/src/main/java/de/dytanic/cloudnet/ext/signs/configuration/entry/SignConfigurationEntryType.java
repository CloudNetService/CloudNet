package de.dytanic.cloudnet.ext.signs.configuration.entry;


public enum SignConfigurationEntryType {
  BUKKIT {
    public SignConfigurationEntry createEntry(String targetGroup) {
      return SignConfigurationEntry
        .createDefault(targetGroup, "GOLD_BLOCK", "EMERALD_BLOCK", "BEDROCK", "REDSTONE_BLOCK");
    }
  },
  NUKKIT {
    public SignConfigurationEntry createEntry(String targetGroup) {
      return SignConfigurationEntry.createDefault(targetGroup, "41", "133", "7", "152");
    }
  };

  public abstract SignConfigurationEntry createEntry(String targetGroup);

}
