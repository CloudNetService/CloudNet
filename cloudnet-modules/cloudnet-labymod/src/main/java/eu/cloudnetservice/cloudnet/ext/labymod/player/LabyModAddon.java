package eu.cloudnetservice.cloudnet.ext.labymod.player;

import java.util.UUID;

public class LabyModAddon {
    private UUID uuid;
    private String name;

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
