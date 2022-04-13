package dev.cromo29.terrains.events;

import com.sk89q.worldguard.protection.managers.RegionManager;
import dev.cromo29.durkcore.Util.TXT;
import dev.cromo29.terrains.TerrainPlugin;
import dev.cromo29.terrains.api.TerrainAPI;
import dev.cromo29.terrains.object.Terrain;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class DelRegionEvent implements Listener {

    private final TerrainPlugin plugin;
    private final TerrainAPI terrainAPI;

    public DelRegionEvent(TerrainPlugin plugin) {
        this.plugin = plugin;
        this.terrainAPI = plugin.getTerrainAPI();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreProcessCommand(PlayerCommandPreprocessEvent event) {

        if (event.isCancelled()) return;

        handle(event.getPlayer(), event.getMessage().toLowerCase());

    }

    private void handle(Player player, String command) {
        if (!command.startsWith("/rg ") && !command.startsWith("/region ") || !player.hasPermission("29Terrains.*"))
            return;

        final String[] args = command.split(" ");

        if (args.length != 3) return;

        if (!args[1].equalsIgnoreCase("remove")
                && !args[1].equalsIgnoreCase("rem")
                && !args[1].equalsIgnoreCase("delete")
                && !args[1].equalsIgnoreCase("del")) return;

        final String region = args[2];

        if (!region.contains("-") || region.split("-").length != 2) return;

        final String owner = region.split("-")[0];
        final String terrainName = region.split("-")[1];

        final RegionManager regionManager = plugin.getWorldGuard().getRegionManager(player.getWorld());

        if (!regionManager.hasRegion(region)) return;

        final Terrain terrain = terrainAPI.getTerrain(terrainName, owner);

        if (terrain == null) return;

        terrain.deleteAsync();

        TXT.sendMessages(player, " <2>✔ <a>Terreno <f>" + terrain.getName() + " <a>de <f>" + terrain.getOwner() + " <a>encontrado e deletado!");

        player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
    }
}
