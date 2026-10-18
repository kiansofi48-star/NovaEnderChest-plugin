package ir.novapixel.novaenderchest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public class EnderChestManager {

    private final JavaPlugin plugin;

    private Connection connection;

    private final Map<UUID, InventorySession> sessions =
            new HashMap<>();

    public EnderChestManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {

        try {

            File folder = plugin.getDataFolder();

            if (!folder.exists()) {
                folder.mkdirs();
            }

            File database =
                    new File(folder, "enderchest.db");

            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" +
                            database.getAbsolutePath()
            );

            try (Statement statement =
                         connection.createStatement()) {

                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS enderchest (
                            uuid TEXT NOT NULL,
                            level INTEGER NOT NULL,
                            slot INTEGER NOT NULL,
                            item BLOB,
                            PRIMARY KEY (uuid, level, slot)
                        )
                        """);
            }

            plugin.getLogger().info(
                    "NovaEnderChest SQLite database loaded."
            );

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Could not initialize database!"
            );

            e.printStackTrace();
        }
    }

    public int getPlayerLevel(Player player) {

        int highest = 0;

        for (int level = 1; level <= 5; level++) {

            String permission =
                    plugin.getConfig().getString(
                            "levels." +
                                    level +
                                    ".permission"
                    );

            if (permission != null &&
                    player.hasPermission(permission)) {

                highest = level;
            }
        }

        return highest;
    }

    public int getRows(int level) {

        return plugin.getConfig().getInt(
                "levels." +
                        level +
                        ".rows",
                1
        );
    }

    public Inventory open(Player player) {

        UUID uuid = player.getUniqueId();

        /*
         * اگر قبلاً GUI باز دارد،
         * همان GUI استفاده شود.
         */

        InventorySession old =
                sessions.get(uuid);

        if (old != null) {

            player.openInventory(old.inventory());

            return old.inventory();
        }

        int level = getPlayerLevel(player);

        if (level <= 0) {
            return null;
        }

        int rows = getRows(level);

        String title =
                plugin.getConfig().getString(
                        "messages.title",
                        "&8Nova Ender Chest &7- Level %level%"
                );

        title = title.replace(
                "%level%",
                String.valueOf(level)
        );

        title =
                ChatColor.translateAlternateColorCodes(
                        '&',
                        title
                );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        rows * 9,
                        title
                );

        loadItems(
                uuid,
                level,
                inventory
        );

        sessions.put(
                uuid,
                new InventorySession(
                        inventory,
                        level
                )
        );

        player.openInventory(inventory);

        player.sendMessage(
                color(
                        plugin.getConfig().getString(
                                "messages.opened"
                        ).replace(
                                "%level%",
                                String.valueOf(level)
                        )
                )
        );

        return inventory;
    }

    private void loadItems(
            UUID uuid,
            int level,
            Inventory inventory
    ) {

        if (connection == null) {
            return;
        }

        String sql = """
                SELECT slot, item
                FROM enderchest
                WHERE uuid = ? AND level = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    uuid.toString()
            );

            statement.setInt(
                    2,
                    level
            );

            try (ResultSet result =
                         statement.executeQuery()) {

                while (result.next()) {

                    int slot =
                            result.getInt("slot");

                    byte[] data =
                            result.getBytes("item");

                    if (slot < 0 ||
                            slot >= inventory.getSize()) {
                        continue;
                    }

                    if (data == null) {
                        continue;
                    }

                    try {

                        ItemStack item =
                                ItemStack.deserializeBytes(
                                        data
                                );

                        inventory.setItem(
                                slot,
                                item
                        );

                    } catch (Exception ignored) {
                        // Invalid item data is ignored.
                    }
                }
            }

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Could not load Ender Chest."
            );

            e.printStackTrace();
        }
    }

    public void save(
            Player player,
            Inventory inventory
    ) {

        UUID uuid =
                player.getUniqueId();

        InventorySession session =
                sessions.get(uuid);

        if (session == null ||
                session.inventory() != inventory) {
            return;
        }

        int level =
                session.level();

        if (connection == null) {
            return;
        }

        /*
         * اول اطلاعات قبلی این Level حذف می‌شود.
         */

        try {

            connection.setAutoCommit(false);

            try (PreparedStatement delete =
                         connection.prepareStatement(
                                 """
                                 DELETE FROM enderchest
                                 WHERE uuid = ? AND level = ?
                                 """
                         )) {

                delete.setString(
                        1,
                        uuid.toString()
                );

                delete.setInt(
                        2,
                        level
                );

                delete.executeUpdate();
            }

            /*
             * سپس وضعیت فعلی Inventory ذخیره می‌شود.
             */

            try (PreparedStatement insert =
                         connection.prepareStatement(
                                 """
                                 INSERT INTO enderchest
                                 (uuid, level, slot, item)
                                 VALUES (?, ?, ?, ?)
                                 """
                         )) {

                for (int slot = 0;
                     slot < inventory.getSize();
                     slot++) {

                    ItemStack item =
                            inventory.getItem(slot);

                    if (item == null ||
                            item.getType().isAir()) {
                        continue;
                    }

                    insert.setString(
                            1,
                            uuid.toString()
                    );

                    insert.setInt(
                            2,
                            level
                    );

                    insert.setInt(
                            3,
                            slot
                    );

                    insert.setBytes(
                            4,
                            item.serializeAsBytes()
                    );

                    insert.addBatch();
                }

                insert.executeBatch();
            }

            connection.commit();

        } catch (SQLException e) {

            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }

            plugin.getLogger().severe(
                    "Could not save Ender Chest!"
            );

            e.printStackTrace();

        } finally {

            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }

        sessions.remove(uuid);
    }

    public boolean isNovaInventory(
            Player player,
            Inventory inventory
    ) {

        InventorySession session =
                sessions.get(
                        player.getUniqueId()
                );

        return session != null &&
                session.inventory() == inventory;
    }

    public void close(Player player) {

        UUID uuid =
                player.getUniqueId();

        InventorySession session =
                sessions.get(uuid);

        if (session == null) {
            return;
        }

        save(
                player,
                session.inventory()
        );
    }

    public void closeAll() {

        /*
         * کپی می‌گیریم تا هنگام remove
         * مشکل ConcurrentModification پیش نیاید.
         */

        List<Player> players =
                new ArrayList<>(
                        Bukkit.getOnlinePlayers()
                );

        for (Player player : players) {
            close(player);
        }
    }

    public void closeDatabase() {

        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException ignored) {
        }
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

    public record InventorySession(
            Inventory inventory,
            int level
    ) {
    }
}