package dev.cromo29.terrains.events;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import dev.cromo29.terrains.TerrainPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class BreakPlatationEvent implements Listener {

    private final TerrainPlugin plugin;

    public BreakPlatationEvent(TerrainPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreakPlantation(PlayerInteractEvent event) {

        handle(event);

    }

    private void handle(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;

        final RegionManager regionManager = plugin.getWorldGuard().getRegionManager(event.getPlayer().getWorld());
        final ApplicableRegionSet set = regionManager.getApplicableRegions(event.getPlayer().getLocation());

        if (set.size() == 0) return;

        event.setCancelled(true);
    }
}
