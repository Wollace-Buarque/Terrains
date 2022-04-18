package dev.cromo29.terrains.object;

import dev.cromo29.durkcore.Util.TXT;
import dev.cromo29.terrains.TerrainPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Terrain {

    private final TerrainPlugin plugin = TerrainPlugin.get();

    private final String owner, name;
    private final int size;
    private long expiration;
    private boolean blocked, automatic;

    public Terrain(String owner, String name, int size) {
        this.owner = owner;
        this.name = name;
        this.size = size;
        this.expiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30);
        this.blocked = false;
        this.automatic = false;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public int getSize() {
        return size;
    }

    public String getSizeMessage() {
        return size + "x" + size;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiration;
    }

    public void updateExpiration() {
        /*
        expiration += TimeUnit.DAYS.toMillis(30);
        if (!isExpired()) blocked = false;

        saveAsync();

        Não pagar todas as dívidas caso estejam vencidas.
         */

        expiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30);
        blocked = false;

        saveAsync();
    }

    public double getIPTUPrice() {
        return size * 68;
    }

    public void block() {
        blocked = true;

        if (!plugin.getData().contains("terrains." + owner.toLowerCase() + "." + name.toLowerCase())) return;

        saveAsync();
    }

    public void deleteAsync() {
        plugin.getTerrainAPI().removeTerrain(owner, this);

        String path = "terrains." + owner.toLowerCase() + "." + name.toLowerCase();

        if (!plugin.getData().contains(path)) return;

        boolean everything = false;
        for (String owner : plugin.getData().getSection("terrains")) {

            if (!owner.equalsIgnoreCase(this.owner)) continue;

            everything = (long) plugin.getData().getSection("terrains." + owner).size() <= 1;
            break;

        }

        boolean finalEverything = everything;
        TXT.runAsynchronously(plugin, () -> {

            if (finalEverything) plugin.getData().removeAll("terrains." + owner.toLowerCase());
            else plugin.getData().remove(path);

            plugin.getData().save();
        });

    }

    public void saveAsync() {
        TXT.runAsynchronously(plugin, () -> {
            String path = "terrains." + owner.toLowerCase() + "." + name.toLowerCase();

            Map<String, Object> map = new HashMap<>();

            map.put("size", "" + size);
            map.put("expiration", "" + expiration);
            map.put("blocked", blocked);
            map.put("automatic", automatic);

            plugin.getData().put(path, map);
            plugin.getData().save();

        });
    }
}
