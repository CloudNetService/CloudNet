package eu.cloudnetservice.cloudnet.ext.signs.bukkit.functionality;

import com.google.common.collect.ImmutableList;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class CommandSigns implements TabExecutor {

    protected static final String[] HELP = new String[]{
            "/signs create <targetGroup> [templatePath]",
            "/signs remove",
            "/signs removeAll",
            "/signs cleanup"
    };

    protected final ServiceSignManagement<org.bukkit.block.Sign> signManagement;

    public CommandSigns(ServiceSignManagement<org.bukkit.block.Sign> signManagement) {
        this.signManagement = signManagement;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may execute this command");
            return true;
        }

        Player player = (Player) sender;
        SignConfigurationEntry entry = this.signManagement.getApplicableSignConfigurationEntry();
        if (entry == null) {
            this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-no-entry", sender::sendMessage);
            return true;
        }

        if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("create")) {
            Block targetBlock = player.getTargetBlock((Set<Material>) null, 15);
            if (targetBlock.getState() instanceof org.bukkit.block.Sign) {
                Sign sign = this.signManagement.getSignAt((org.bukkit.block.Sign) targetBlock.getState());
                if (sign != null) {
                    this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-sign-already-exist",
                            player::sendMessage, m -> m.replace("%group%", sign.getTargetGroup()));
                    return true;
                }

                Sign createdSign = this.signManagement.createSign(
                        (org.bukkit.block.Sign) targetBlock.getState(),
                        args[1], args.length == 3 ? args[2] : null
                );
                if (createdSign != null) {
                    this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-create-success",
                            player::sendMessage, m -> m.replace("%group%", createdSign.getTargetGroup()));
                }
            } else {
                this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
            }

            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("cleanup")) {
            int removed = this.signManagement.removeMissingSigns();
            this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-cleanup-success", player::sendMessage,
                    m -> m.replace("%amount%", Integer.toString(removed)));
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("removeall")) {
            int removed = this.signManagement.deleteAllSigns();
            this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-bulk-remove-success", player::sendMessage,
                    m -> m.replace("%amount%", Integer.toString(removed)));
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
            Block targetBlock = player.getTargetBlock((Set<Material>) null, 15);
            if (targetBlock.getState() instanceof org.bukkit.block.Sign) {
                Sign sign = this.signManagement.getSignAt((org.bukkit.block.Sign) targetBlock.getState());
                if (sign == null) {
                    this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-remove-not-existing", player::sendMessage);
                } else {
                    this.signManagement.deleteSign(sign);
                    this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-remove-success", player::sendMessage);
                }
            } else {
                this.signManagement.getSignsConfiguration().sendMessage("command-cloudsign-not-looking-at-sign", player::sendMessage);
            }

            return true;
        }

        sender.sendMessage(HELP);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return ImmutableList.of("create", "remove", "removeAll", "cleanup");
        }
        return ImmutableList.of();
    }
}
