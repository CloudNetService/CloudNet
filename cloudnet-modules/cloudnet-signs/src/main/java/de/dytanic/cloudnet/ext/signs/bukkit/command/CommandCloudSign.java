package de.dytanic.cloudnet.ext.signs.bukkit.command;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.ext.signs.*;
import de.dytanic.cloudnet.ext.signs.bukkit.BukkitSignManagement;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandCloudSign implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        SignConfigurationEntry entry = BukkitSignManagement.getInstance().getOwnSignConfigurationEntry();

        if (entry == null) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("§7/cloudsign create <targetGroup>");
            sender.sendMessage("§7/cloudsign create <targetGroup> <templatePath>");
            sender.sendMessage("§7/cloudsign remove");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
            Block block = BukkitSignManagement.getInstance().getLivingEntityTargetBlock(player, 15);

            if (block.getState() instanceof org.bukkit.block.Sign) {
                for (Sign sign : BukkitSignManagement.getInstance().getSigns()) {
                    if (!Iterables.contains(sign.getProvidedGroup(), Wrapper.getInstance().getServiceConfiguration().getGroups())) {
                        continue;
                    }

                    Location location = BukkitSignManagement.getInstance().toLocation(sign.getWorldPosition());

                    if (location != null && location.equals(block.getLocation())) {
                        BukkitSignManagement.getInstance().sendSignRemoveUpdate(sign);

                        org.bukkit.block.Sign blockSign = (org.bukkit.block.Sign) block.getState();
                        blockSign.setLine(0, "");
                        blockSign.setLine(1, "");
                        blockSign.setLine(2, "");
                        blockSign.setLine(3, "");
                        blockSign.update();

                        sender.sendMessage(
                                ChatColor.translateAlternateColorCodes('&',
                                        SignConfigurationProvider.load().getMessages().get("command-cloudsign-remove-success")
                                )
                        );
                        return true;
                    }
                }
            }
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
            Block block = BukkitSignManagement.getInstance().getLivingEntityTargetBlock(player, 15);

            if (block.getState() instanceof org.bukkit.block.Sign) {
                for (Sign sign : BukkitSignManagement.getInstance().getSigns()) {
                    if (!Iterables.contains(sign.getProvidedGroup(), Wrapper.getInstance().getServiceConfiguration().getGroups())) {
                        continue;
                    }

                    Location location = BukkitSignManagement.getInstance().toLocation(sign.getWorldPosition());

                    if (location != null && location.equals(block.getLocation())) {
                        sender.sendMessage(
                                ChatColor.translateAlternateColorCodes('&',
                                        SignConfigurationProvider.load().getMessages().getOrDefault("command-cloudsign-sign-already-exist",
                                                "&7The sign is already set. If you want to remove that, use the /cloudsign remove command")
                                                .replace("%group%", sign.getTargetGroup())
                                )
                        );
                        return true;
                    }
                }

                Sign sign = new Sign(
                        entry.getTargetGroup(),
                        args[1],
                        new SignPosition(block.getX(), block.getY(), block.getZ(), 0, 0, entry.getTargetGroup(), block.getWorld().getName()),
                        args.length == 3 ? args[2] : null
                );

                AbstractSignManagement.getInstance().sendSignAddUpdate(sign);
                sender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&',
                                SignConfigurationProvider.load().getMessages().get("command-cloudsign-create-success")
                                        .replace("%group%", sign.getTargetGroup())
                        )
                );
            }
        }

        return true;
    }
}