package de.dytanic.cloudnet.ext.signs.nukkit.command;


import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Location;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignPosition;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.nukkit.NukkitSignManagement;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Arrays;


public class CommandCloudSign extends Command {

    public CommandCloudSign() {
        super("cloudsign", "Add or Removes signs from the provided Group configuration", "/cloudsign create <targetGroup>", new String[]{"cs"});
        this.setPermission("cloudnet.command.cloudsign");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!super.testPermission(sender)) {
            return true;
        }

        if (!(sender instanceof Player)) {
            return false;
        }

        SignConfigurationEntry entry = AbstractSignManagement.getInstance().getOwnSignConfigurationEntry();

        if (entry == null) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("§7/cloudsign create <targetGroup>");
            sender.sendMessage("§7/cloudsign create <targetGroup> <templatePath>");
            sender.sendMessage("§7/cloudsign remove");
            sender.sendMessage("§7/cloudsign cleanup");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
            Block block = player.getTargetBlock(15);
            BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getLocation());

            if (blockEntity instanceof BlockEntitySign) {
                for (Sign sign : AbstractSignManagement.getInstance().getSigns()) {
                    if (!Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(sign.getProvidedGroup())) {
                        continue;
                    }

                    Location location = NukkitSignManagement.getInstance().toLocation(sign.getWorldPosition());

                    if (location != null && location.equals(block.getLocation())) {
                        AbstractSignManagement.getInstance().sendSignRemoveUpdate(sign);

                        BlockEntitySign blockSign = (BlockEntitySign) blockEntity;
                        blockSign.setText();

                        sender.sendMessage(
                                SignConfigurationProvider.load().getMessages().get("command-cloudsign-remove-success")
                                        .replace('&', '§')
                        );
                        return true;
                    }
                }
            }
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
            Block block = player.getTargetBlock(15);
            BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getLocation());

            if (blockEntity instanceof BlockEntitySign) {
                for (Sign sign : AbstractSignManagement.getInstance().getSigns()) {
                    if (!Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(sign.getProvidedGroup())) {
                        continue;
                    }

                    Location location = NukkitSignManagement.getInstance().toLocation(sign.getWorldPosition());

                    if (location != null && location.equals(block.getLocation())) {
                        sender.sendMessage(
                                SignConfigurationProvider.load().getMessages().getOrDefault("command-cloudsign-sign-already-exist",
                                        "&7The sign is already set. If you want to remove that, use the /cloudsign remove command")
                                        .replace("%group%", sign.getTargetGroup())
                                        .replace('&', '§')
                        );
                        return true;
                    }
                }

                Sign sign = new Sign(
                        entry.getTargetGroup(),
                        args[1],
                        new SignPosition(block.getX(), block.getY(), block.getZ(), 0, 0, entry.getTargetGroup(), block.getLevel().getName()),
                        args.length == 3 ? args[2] : null
                );

                AbstractSignManagement.getInstance().sendSignAddUpdate(sign);
                sender.sendMessage(
                        SignConfigurationProvider.load().getMessages().get("command-cloudsign-create-success")
                                .replace("%group%", sign.getTargetGroup())
                                .replace('&', '§')
                );
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("cleanup")) {
            AbstractSignManagement.getInstance().cleanup();

            sender.sendMessage(
                    SignConfigurationProvider.load().getMessages().get("command-cloudsign-cleanup-success").replace('&', '§')
            );
        }

        return true;
    }


}
