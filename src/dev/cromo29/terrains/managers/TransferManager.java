package dev.cromo29.terrains.managers;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.cromo29.durkcore.util.TXT;
import dev.cromo29.terrains.TerrainPlugin;
import dev.cromo29.terrains.object.Terrain;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.Map;

public class TransferManager {

    public static void transfer(String worldName) {
        final TerrainPlugin plugin = TerrainPlugin.get();
        final World world = plugin.getServer().getWorld(worldName);

        if (world == null) {
            plugin.log(" <c>Mundo <f>" + worldName + " <c>nao encontrado!");
            return;
        }

        final RegionManager regionManager = plugin.getWorldGuard().getRegionManager(world);
        final Map<String, ProtectedRegion> regions = regionManager.getRegions();

        TXT.runAsynchronously(plugin, () -> {

            regions.forEach((dices, protectedRegion) -> {
                if (!dices.contains("-") || dices.split("-").length < 2) return;

                final String owner = dices.split("-")[0].toLowerCase();
                final String terrainName = dices.split("-")[1].toLowerCase();

                final OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(owner);

                if (!offlinePlayer.hasPlayedBefore()) return;

                final int size = (int) (protectedRegion.getMaximumPoint().getX() - protectedRegion.getMinimumPoint().getX());

                final Terrain terrain = new Terrain(owner, terrainName, size);
                plugin.getTerrainAPI().addTerrain(owner, terrain);

                terrain.saveAsync();
            });

        });
    }
}
