package dev.cromo29.terrains.events.core;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.cromo29.terrains.object.Terrain;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DeleteTerrainEvent extends Event implements Cancellable {

    private final Player player;
    private final Terrain terrain;
    private final RegionManager regionManager;
    private final ProtectedRegion protectedRegion;
    private final Location location;

    private boolean cancelled = false;

    public DeleteTerrainEvent(Player player, Terrain terrain, RegionManager regionManager, ProtectedRegion protectedRegion, Location location) {
        this.player = player;
        this.terrain = terrain;
        this.regionManager = regionManager;
        this.protectedRegion = protectedRegion;
        this.location = location;
    }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public Player getPlayer() { return player; }

    public Terrain getTerrain() {
        return terrain;
    }

    public Location getLocation() {
        return location;
    }

    // Don't use the method 'save', the plugin already will save.
    public RegionManager getRegionManager() {
        return regionManager;
    }

    public ProtectedRegion getProtectedRegion() {
        return protectedRegion;
    }

    private static HandlerList handlerList = new HandlerList();
    public HandlerList getHandlers() {return handlerList;}
    public static HandlerList getHandlerList()  {return handlerList;}
}
