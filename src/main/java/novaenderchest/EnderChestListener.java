package ir.novapixel.novaenderchest;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public class EnderChestListener implements Listener {

    private final JavaPlugin plugin;
    private final EnderChestManager manager;

    public EnderChestListener(
            JavaPlugin plugin,
            EnderChestManager manager
    ) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * کلیک روی Ender Chest روی زمین
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEnderChestClick(
            PlayerInteractEvent event
    ) {

        if (!plugin.getConfig().getBoolean(
                "settings.chest-interaction",
                true
        )) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block =
                event.getClickedBlock();

        if (block == null) {
            return;
        }

        if (block.getType() != Material.ENDER_CHEST) {
            return;
        }

        Player player =
                event.getPlayer();

        /*
         * جلوی باز شدن Ender Chest اصلی Minecraft
         * گرفته می‌شود.
         */

        event.setCancelled(true);

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

            return;
        }

        if (manager.getPlayerLevel(player) <= 0) {

            player.sendMessage(
                    color(
                            plugin.getConfig().getString(
                                    "messages.no-permission"
                            )
                    )
            );

            return;
        }

        manager.open(player);
    }

    /**
     * جلوگیری از بعضی تعاملات ناخواسته در GUI
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        Inventory top =
                event.getView().getTopInventory();

        if (!manager.isNovaInventory(
                player,
                top
        )) {
            return;
        }

        /*
         * هیچ آیتمی اینجا دستی add/remove نمی‌شود.
         *
         * انتقال معمولی توسط خود Minecraft/Paper
         * انجام می‌شود.
         *
         * این موضوع برای جلوگیری از Dupe مهم است.
         */
    }

    /**
     * Drag معمولی
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onDrag(
            InventoryDragEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        Inventory top =
                event.getView().getTopInventory();

        if (!manager.isNovaInventory(
                player,
                top
        )) {
            return;
        }

        /*
         * هیچ تغییر دستی روی آیتم‌ها انجام نمی‌دهیم.
         */
    }

    /**
     * ذخیره هنگام بستن
     */
    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onClose(
            InventoryCloseEvent event
    ) {

        if (!(event.getPlayer()
                instanceof Player player)) {
            return;
        }

        Inventory top =
                event.getView().getTopInventory();

        if (!manager.isNovaInventory(
                player,
                top
        )) {
            return;
        }

        manager.save(
                player,
                top
        );
    }

    /**
     * ذخیره هنگام خروج
     */
    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        manager.close(
                event.getPlayer()
        );
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