package dev.cromo29.terrains.events;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.cromo29.durkcore.util.TXT;
import dev.cromo29.terrains.TerrainPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class SetHomeEvent implements Listener {

    private final TerrainPlugin plugin;

    public SetHomeEvent(TerrainPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreProcessCommand(PlayerCommandPreprocessEvent event) {

        handle(event.getPlayer(), event);

    }

    public void handle(Player player, PlayerCommandPreprocessEvent event) {
        final String command = event.getMessage().toLowerCase();

        if (!command.startsWith("/sethome") || player.hasPermission("29Terrains.*")) return;

        final RegionManager regionManager = plugin.getWorldGuard().getRegionManager(player.getWorld());
        final ApplicableRegionSet set = regionManager.getApplicableRegions(player.getLocation());

        if (set.size() == 0) return;

        final String id = set.iterator().next().getId();
        final ProtectedRegion protectedRegion = regionManager.getRegion(id);

        if (protectedRegion == null) return;

        boolean isMember = protectedRegion.isMember(plugin.getWorldGuard().wrapPlayer(player));

        if (isMember) return;

        event.setCancelled(true);

        TXT.sendMessages(player, " <6>☣ <c>Você só pode definir casas em áreas que você tem permissão ou não tenha proteção.");
    }
}
