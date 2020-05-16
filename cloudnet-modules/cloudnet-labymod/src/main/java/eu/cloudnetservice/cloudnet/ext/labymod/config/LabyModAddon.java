package eu.cloudnetservice.cloudnet.ext.labymod.config;

import java.util.UUID;

public class LabyModAddon {
    private final UUID uuid;
    private final String name;

    public LabyModAddon(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }
}
