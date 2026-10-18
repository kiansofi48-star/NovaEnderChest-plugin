package ir.novapixel.novaenderchest;

import org.bukkit.plugin.java.JavaPlugin;

public final class NovaEnderChest extends JavaPlugin {

    private EnderChestManager manager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        manager = new EnderChestManager(this);

        manager.initDatabase();

        getCommand("ec").setExecutor(
                new EnderChestCommand(this, manager)
        );

        getCommand("nec").setExecutor(
                new EnderChestCommand(this, manager)
        );

        getServer().getPluginManager().registerEvents(
                new EnderChestListener(this, manager),
                this
        );

        getLogger().info("NovaEnderChest enabled!");
    }

    @Override
    public void onDisable() {

        if (manager != null) {
            manager.closeAll();
            manager.closeDatabase();
        }

        getLogger().info("NovaEnderChest disabled!");
    }

    public EnderChestManager getManager() {
        return manager;
    }
}