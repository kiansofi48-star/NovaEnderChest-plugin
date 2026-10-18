package ir.novapixel.novaenderchest;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EnderChestCommand implements CommandExecutor {

    private final NovaEnderChest plugin;
    private final EnderChestManager manager;

    public EnderChestCommand(
            NovaEnderChest plugin,
            EnderChestManager manager
    ) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    color(
                            plugin.getConfig().getString(
                                    "messages.only-player"
                            )
                    )
            );

            return true;
        }

        if (!player.hasPermission(
                "nova.enderchest.use"
        )) {

            player.sendMessage(
                    color(
                            plugin.getConfig().getString(
                                    "messages.no-permission"
                            )
                    )
            );

            return true;
        }

        /*
         * فقط /ec open و /nec open
         */

        if (args.length != 1 ||
                !args[0].equalsIgnoreCase("open")) {

            player.sendMessage(
                    ChatColor.RED +
                            "استفاده صحیح: /" +
                            label +
                            " open"
            );

            return true;
        }

        int level = manager.getPlayerLevel(player);

        if (level <= 0) {

            player.sendMessage(
                    color(
                            plugin.getConfig().getString(
                                    "messages.no-permission"
                            )
                    )
            );

            return true;
        }

        manager.open(player);

        return true;
    }

    private String color(String text) {

        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}