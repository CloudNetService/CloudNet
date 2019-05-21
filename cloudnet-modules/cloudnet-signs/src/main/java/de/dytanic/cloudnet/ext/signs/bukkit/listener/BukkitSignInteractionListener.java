package de.dytanic.cloudnet.ext.signs.bukkit.listener;

import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.bukkit.BukkitSignManagement;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class BukkitSignInteractionListener implements Listener {

    @EventHandler
    public void handle(PlayerInteractEvent event)
    {
        SignConfigurationEntry entry = BukkitSignManagement.getInstance().getOwnSignConfigurationEntry();

        if (entry != null)
            if ((event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) &&
                event.getClickedBlock() != null &&
                event.getClickedBlock().getState() instanceof org.bukkit.block.Sign)
                for (Sign sign : BukkitSignManagement.getInstance().getSigns())
                {
                    Location location = BukkitSignManagement.getInstance().toLocation(sign.getWorldPosition());

                    if (location == null || sign.getServiceInfoSnapshot() == null ||
                        !location.equals(event.getClickedBlock().getLocation()))
                        continue;

                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                         DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream))
                    {
                        dataOutputStream.writeUTF("Connect");
                        dataOutputStream.writeUTF(sign.getServiceInfoSnapshot().getServiceId().getName());
                        event.getPlayer().sendPluginMessage(BukkitSignManagement.getInstance().getPlugin(), "BungeeCord", byteArrayOutputStream.toByteArray());

                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    event.getPlayer().sendMessage(
                        ChatColor.translateAlternateColorCodes('&',
                            SignConfigurationProvider.load().getMessages().get("server-connecting-message")
                                .replace("%server%", sign.getServiceInfoSnapshot().getServiceId().getName())
                        )
                    );

                    return;
                }
    }
}