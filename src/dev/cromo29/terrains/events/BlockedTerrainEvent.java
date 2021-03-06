package dev.cromo29.terrains.events;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.cromo29.durkcore.util.TXT;
import dev.cromo29.terrains.api.TerrainAPI;
import dev.cromo29.terrains.object.Terrain;
import dev.cromo29.terrains.TerrainPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

public class BlockedTerrainEvent implements Listener {

    private final TerrainPlugin plugin;
    private final TerrainAPI terrainAPI;

    public BlockedTerrainEvent(TerrainPlugin plugin) {
        this.plugin = plugin;
        this.terrainAPI = plugin.getTerrainAPI();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractEvent(PlayerInteractEvent event) {

        if (!event.hasBlock() || event.getClickedBlock().getType() == Material.AIR) return;

        final Player player = event.getPlayer();
        final RegionManager regionManager = plugin.getWorldGuard().getRegionManager(player.getWorld());
        ApplicableRegionSet set;

        Location location = player.getTargetBlock((Set<Material>) null, 5).getLocation();

        if (location == null || location.getBlock() == null || location.getBlock().getType() == Material.AIR) return;

        set = regionManager.getApplicableRegions(location);

        if (set.size() == 0) set = regionManager.getApplicableRegions(player.getLocation());
        if (set.size() == 0) return;

        final String id = set.iterator().next().getId();
        final ProtectedRegion protectedRegion = regionManager.getRegion(id);

        if (protectedRegion == null) return;

        final String owner = protectedRegion.getOwners().toUserFriendlyString().replace("name:", "");
        final String areaName = id.split("_")[id.split("_").length - 1].replace(owner.toLowerCase() + "-", "");

        final Terrain terrain = terrainAPI.getTerrain(areaName, owner);

        if (terrain == null) return;

        boolean isMember = protectedRegion.isMember(plugin.getWorldGuard().wrapPlayer(player));

        if (!player.getName().equalsIgnoreCase(owner) && !isMember) return;

        if (terrain.isBlocked()) {
            event.setCancelled(true);

            if (isMember)
                TXT.sendMessages(player, " <6>??? <c>Ningu??m pode mexer no terreno at?? o <f>IPTU<c> ser pago!");
            else TXT.sendMessages(player, " <6>??? <c>Voc?? precisa pagar o <f>IPTU <c>para mexer neste terreno!");
        }
    }
}
