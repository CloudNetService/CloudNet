package de.dytanic.cloudnet.examples.sign;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignLayout;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationTaskEntry;
import org.bukkit.Material;

public final class ExampleSigns {

    // getting the SignManagement via CloudNet's service registry
    private AbstractSignManagement signManagement = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(AbstractSignManagement.class);

    public void updateSigns() {
        this.signManagement.updateSigns();
    }

    public void foreachSigns() {
        for (Sign sign : this.signManagement.getSigns()) {
            // ...
        }
    }

    public void customizeSignLayout() {
        this.signManagement.getOwnSignConfigurationEntry().getTaskLayouts().add(new SignConfigurationTaskEntry(
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