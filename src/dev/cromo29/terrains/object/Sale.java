package dev.cromo29.terrains.object;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.cromo29.durkcore.Util.TXT;
import dev.cromo29.terrains.TerrainPlugin;
import org.bukkit.entity.Player;

public class Sale {

    private static final TerrainPlugin PLUGIN = TerrainPlugin.get();

    private final String owner, buyer, terrain;
    private final RegionManager regionManager;
    private final ProtectedRegion region;
    private double price;

    public Sale(String owner, String buyer, String terrain, RegionManager regionManager, ProtectedRegion region, double price) {
        this.owner = owner;
        this.buyer = buyer;
        this.terrain = terrain;
        this.regionManager = regionManager;
        this.region = region;
        this.price = price;
    }

    public String getOwner() {
        return owner;
    }

    public String getBuyer() {
        return buyer;
    }

    public String getTerrain() {
        return terrain;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public void updateConfig() {
        Terrain currentTerrain = PLUGIN.getTerrainAPI().getTerrain(terrain, owner);
        PLUGIN.getTerrainAPI().removeTerrain(owner, currentTerrain);

        if (currentTerrain == null) return;

        currentTerrain.deleteAsync();

        Terrain buyerTerrain = new Terrain(buyer, terrain, currentTerrain.getSize());

        buyerTerrain.setExpiration(currentTerrain.getExpiration());
        buyerTerrain.setBlocked(currentTerrain.isBlocked());
        buyerTerrain.saveAsync();

        PLUGIN.getTerrainAPI().addTerrain(buyer, buyerTerrain);
    }

    public void selfDestruct() {
        Sale sale = this;

        TXT.runLaterAsynchronously(PLUGIN, 1200, () -> {

            if (!PLUGIN.getSaleList().contains(sale)) return;

            delete();

            Player ownerPlayer = PLUGIN.getServer().getPlayer(owner);
            Player buyerPlayer = PLUGIN.getServer().getPlayer(buyer);

            if (ownerPlayer != null) {
                TXT.sendMessages(ownerPlayer,
                        "",
                        " <6>☣ <c>Se passaram <f>1 minuto <c>e <f>" + buyer + " <c>não aceitou sua proposta.",
                        " <6>☣ <c>Venda cancelada automáticamente!",
                        "");
            }

            if (buyerPlayer != null) {
                TXT.sendMessages(buyerPlayer,
                        "",
                        " <6>☣ <c>Se passaram <f>1 minuto <c>e você não aceitou a proposta de <f>" + owner + "<c>.",
                        " <6>☣ <c>Venda cancelada automáticamente!",
                        "");
            }

        });
    }

    public void delete() {
        PLUGIN.getSaleList().remove(this);
    }
}
