package com.example.mass;

import com.example.mass.command.MassCommand;
import com.example.mass.config.MassConfig;
import com.example.mass.listener.LootListener;
import com.example.mass.listener.PlayerListener;
import com.example.mass.listener.VehicleListener;
import com.example.mass.listener.VillagerListener;
import com.example.mass.service.EncumbranceService;
import com.example.mass.service.LoreService;
import com.example.mass.service.WeightService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class MassPlugin extends JavaPlugin {

    private MassConfig         massConfig;
    private WeightService      weightService;
    private EncumbranceService encumbranceService;
    private LoreService        loreService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        massConfig = new MassConfig(getConfig(), getLogger());

        weightService      = new WeightService(this);
        encumbranceService = new EncumbranceService(this, weightService);
        loreService        = new LoreService(this, weightService);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(encumbranceService, loreService), this);
        pm.registerEvents(new VehicleListener(this, encumbranceService, weightService), this);
        pm.registerEvents(new VillagerListener(this), this);
        pm.registerEvents(new LootListener(this), this);

        MassCommand cmd = new MassCommand(this, weightService, encumbranceService, loreService);
        getServer().getCommandMap().register("mass", "mass", cmd);

        for (Player player : getServer().getOnlinePlayers()) {
            encumbranceService.scheduleUpdate(player);
        }

        // Water sink tick — runs every 4 ticks, pushes heavy players down in water
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.isInWater()) encumbranceService.tickWater(p);
            }
        }, 5L, 4L);

        if (com.example.mass.enchant.LighteningEnchant.get() == null) {
            getLogger().warning("Lightening enchantment not registered — check bootstrap.");
        }
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            encumbranceService.cleanup(player);
        }
    }

    public void reload() {
        reloadConfig();
        massConfig = new MassConfig(getConfig(), getLogger());

        weightService.reloadConfig();
        encumbranceService.reloadConfig();
        loreService.reloadConfig();

        for (Player player : getServer().getOnlinePlayers()) {
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (ItemStack item : contents) if (item != null) loreService.updateLore(item);
            player.getInventory().setStorageContents(contents);

            ItemStack[] armor = player.getInventory().getArmorContents();
            for (ItemStack piece : armor) if (piece != null) loreService.updateLore(piece);
            player.getInventory().setArmorContents(armor);

            encumbranceService.scheduleUpdate(player);
        }
    }

    public @NotNull MassConfig massConfig()                  { return massConfig; }
    public @NotNull WeightService weightService()            { return weightService; }
    public @NotNull EncumbranceService encumbranceService()  { return encumbranceService; }
    public @NotNull LoreService loreService()                { return loreService; }
}
