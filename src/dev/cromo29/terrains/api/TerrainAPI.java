package dev.cromo29.terrains.api;

import dev.cromo29.terrains.TerrainPlugin;
import dev.cromo29.terrains.object.Sale;
import dev.cromo29.terrains.object.Terrain;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TerrainAPI {

    private final TerrainPlugin plugin;

    public TerrainAPI(TerrainPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isCorrectWorld(World world) {
        return world.getName().equalsIgnoreCase(plugin.getConfig().getString("Settings.World"));
    }

    public List<Terrain> getTerrains() {
        return plugin.getTerrainMap().values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<Terrain> getTerrains(String owner) {
        return plugin.getTerrainMap().getOrDefault(owner.toLowerCase(), new ArrayList<>());
    }

    public void addTerrain(String owner, Terrain terrain) {
        List<Terrain> terrains = getTerrains(owner);
        terrains.add(terrain);

        plugin.getTerrainMap().put(owner.toLowerCase(), terrains);
    }

    public void removeTerrain(String owner, Terrain terrain) {
        List<Terrain> terrains = getTerrains(owner);

        if (!terrains.contains(terrain)) return;

        terrains.remove(terrain);

        if (!terrains.isEmpty()) plugin.getTerrainMap().put(owner.toLowerCase(), terrains);
        else plugin.getTerrainMap().remove(owner.toLowerCase());
    }

    public boolean hasTerrain(String terrainName, String owner) {
        return getTerrains(owner)
                .stream()
                .anyMatch(terrain -> terrain.getName().equalsIgnoreCase(terrainName));
    }

    public Terrain getTerrain(String terrainName, String owner) {
        return getTerrains(owner)
                .stream()
                .filter(terrain -> terrain.getName().equalsIgnoreCase(terrainName))
                .findFirst().orElse(null);
    }

    public Sale getSale(String owner, String buyer) {
        return plugin.getSaleList()
                .stream()
                .filter(sale -> sale.getOwner().equalsIgnoreCase(owner) && sale.getBuyer().equalsIgnoreCase(buyer))
                .findFirst().orElse(null);
    }

    public Sale getSaleWithTerrain(String owner, String terrain) {
        return plugin.getSaleList()
                .stream()
                .filter(sale -> sale.getOwner().equalsIgnoreCase(owner) && sale.getTerrain().equalsIgnoreCase(terrain))
                .findFirst().orElse(null);
    }

    public boolean canPurchase(Player player) {
        final int maxArea = plugin.getConfig().getInt("Settings.Max areas");
        final int terrainsSize = getTerrains(player.getDisplayName()).size();

        if (player.isOp() || player.hasPermission("29Terrains.*") || terrainsSize < maxArea) return true;

        boolean hasPermission = false;
        int index = 1;
        while (index < 50) {
            if (player.hasPermission("29Terrains." + index)) {

                if (terrainsSize >= index) return false;

                hasPermission = true;
                break;

            } else ++index;
        }

        return hasPermission;
    }

    public void loadTerrains() {
        plugin.getSaleList().clear();
        plugin.getTerrainMap().clear();

        if (plugin.getData().getSection("terrains") == null) return;

        for (String owner : plugin.getData().getSection("terrains")) {
            for (String terrainName : plugin.getData().getSection("terrains." + owner)) {

                Map<String, Object> map = plugin.getData().get("terrains." + owner + "." + terrainName).asMap();

                final long expiration = Long.parseLong((String) map.get("expiration"));
                final boolean blocked = (boolean) map.get("blocked");
                final int size = Integer.parseInt((String) map.get("size"));
                final boolean automatic = (boolean) map.get("automatic");

                final Terrain terrain = new Terrain(owner, terrainName, size);

                terrain.setExpiration(expiration);
                terrain.setBlocked(blocked);
                terrain.setAutomatic(automatic);

                addTerrain(owner, terrain);
            }
        }
    }
}
