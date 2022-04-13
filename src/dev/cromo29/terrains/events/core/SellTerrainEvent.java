package dev.cromo29.terrains.events.core;

import dev.cromo29.terrains.object.Terrain;
import dev.cromo29.terrains.object.Sale;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SellTerrainEvent extends Event implements Cancellable {

    private final Player owner;
    private final Player buyer;
    private final Terrain terrain;
    private final Sale sale;
    private final Location location;
    private double value;

    private boolean cancelled = false;

    public SellTerrainEvent(Player owner, Player buyer, Terrain terrain, Sale sale, double value, Location location) {
        this.owner = owner;
        this.buyer = buyer;
        this.terrain = terrain;
        this.value = value;
        this.sale = sale;
        this.location = location;
    }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public Player getOwner() { return owner; }
    public Player getBuyer() { return buyer; }

    public Terrain getTerrain() { return terrain; }
    public Sale getSale() { return sale; }

    public Location getLocation() {
        return location;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    private static HandlerList handlerList = new HandlerList();
    public HandlerList getHandlers() {return handlerList;}
    public static HandlerList getHandlerList()  {return handlerList;}
}
