package de.dytanic.cloudnet.examples.sign;

import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignLayout;
import de.dytanic.cloudnet.ext.signs.bukkit.BukkitSignManagement;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationTaskEntry;
import org.bukkit.Material;

public final class ExampleSigns {

    public void updateSigns() {
        BukkitSignManagement.getInstance().updateSigns();
    }

    public void foreachSigns() {
        for (Sign sign : BukkitSignManagement.getInstance().getSigns()) {
            // ...
        }
    }

    public void customizeSignLayout() {
        BukkitSignManagement.getInstance().getOwnSignConfigurationEntry().getTaskLayouts().add(new SignConfigurationTaskEntry(
                "Lobby",
                new SignLayout(
                        new String[]{
                                "Lobby-1",
                                "%online_count% / %max_players%",
                                "A minecraft server",
                                "LOBBY"
                        },
                        Material.STONE.name(),
                        0
                ),
                new SignLayout(
                        new String[]{
                                "Lobby-1",
                                "%online_count% / %max_players%",
                                "A minecraft server",
                                "LOBBY"
                        },
                        Material.STONE.name(),
                        0
                ),
                new SignLayout(
                        new String[]{
                                "Lobby-1",
                                "%online_count% / %max_players%",
                                "A minecraft server",
                                "LOBBY"
                        },
                        Material.STONE.name(),
                        0
                )
        ));
    }
}