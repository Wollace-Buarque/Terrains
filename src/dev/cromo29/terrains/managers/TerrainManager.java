package dev.cromo29.terrains.managers;

import com.sk89q.worldedit.*;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.cromo29.durkcore.SpecificUtils.NumberUtil;
import dev.cromo29.durkcore.SpecificUtils.StringUtil;
import dev.cromo29.durkcore.Util.TXT;
import dev.cromo29.durkcore.Util.TimeFormat;
import dev.cromo29.durkcore.Util.VaultAPI;
import dev.cromo29.terrains.api.TerrainAPI;
import dev.cromo29.terrains.events.core.DeleteTerrainEvent;
import dev.cromo29.terrains.events.core.PurchaseTerrainEvent;
import dev.cromo29.terrains.events.core.SellTerrainEvent;
import dev.cromo29.terrains.object.Terrain;
import dev.cromo29.terrains.object.Sale;
import dev.cromo29.terrains.TerrainPlugin;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class TerrainManager {

    private final TerrainPlugin plugin;
    private final TerrainAPI terrainAPI;

    private final Map<String, String> confirmations;

    private final WorldGuardPlugin worldGuard;
    private final Economy economy;

    public TerrainManager(TerrainPlugin plugin) {
        this.plugin = plugin;
        this.terrainAPI = plugin.getTerrainAPI();

        this.confirmations = new HashMap<>();
        this.worldGuard = plugin.getWorldGuard();
        this.economy = VaultAPI.getEconomy();
    }

    public boolean cantPurchase(Player player, boolean warn) {
        final int maxArea = plugin.getConfig().getInt("Settings.Max areas");
        final int terrainsSize = terrainAPI.getTerrains(player.getDisplayName()).size();

        if (player.isOp() || player.hasPermission("29Terrains.*") || terrainsSize < maxArea) return false;

        boolean hasPermission = false;
        int index = 1;
        while (index < 100) {

            if (player.hasPermission("29Terrains." + index)) {

                if (terrainsSize >= index) {

                    if (warn) {
                        sendMessages(player,
                                Sound.VILLAGER_NO,
                                " <6>☣ <c>Você só pode ter <f>" + index + " <c>terrenos.");
                    }

                    return true;
                }

                hasPermission = true;
                break;

            } else ++index;

        }

        if (!hasPermission) {

            if (warn) {
                sendMessages(player,
                        Sound.VILLAGER_NO,
                        " <6>☣ <c>Você só pode ter <f>" + maxArea + " <c>terrenos.");
            }

            return true;
        }

        return false;
    }

    public void purchase(Player player, int sizeX, int sizeZ, String area) {
        if (!terrainAPI.isCorrectWorld(player.getWorld())) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não é possivel gerenciar terrenos neste mundo.");
            return;
        }

        if (terrainAPI.hasTerrain(area, player.getName())) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você já possui um terreno com o nome <f>" + area + "<c>!");
            return;
        }

        final RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());
        final String id = player.getName().toLowerCase() + "-" + area;

        final int initialPositionX = player.getLocation().getBlockX() - sizeX / 2;
        final int initialPositionZ = player.getLocation().getBlockZ() - sizeZ / 2;
        final int finalPositionX = player.getLocation().getBlockX() + sizeX / 2;
        final int finalPositionZ = player.getLocation().getBlockZ() + sizeZ / 2;

        final BlockVector blockVectorInitial = new BlockVector(initialPositionX, 256, initialPositionZ);
        final BlockVector blockVectorFinal = new BlockVector(finalPositionX, 0, finalPositionZ);

        final ProtectedCuboidRegion protectedCuboidRegion = new ProtectedCuboidRegion(id, blockVectorInitial, blockVectorFinal);
        final DefaultDomain defaultDomain = new DefaultDomain();

        regionManager.addRegion(protectedCuboidRegion);
        protectedCuboidRegion.setPriority(100);
        defaultDomain.addPlayer(player.getName());
        protectedCuboidRegion.setOwners(defaultDomain);

        try {
            protectedCuboidRegion.setFlag(DefaultFlag.PVP, DefaultFlag.PVP.parseInput(worldGuard, player, "allow"));
            protectedCuboidRegion.setFlag(DefaultFlag.USE, DefaultFlag.USE.parseInput(worldGuard, player, "allow"));
            protectedCuboidRegion.setFlag(DefaultFlag.ENDER_BUILD, DefaultFlag.ENDER_BUILD.parseInput(worldGuard, player, "deny"));
            protectedCuboidRegion.setFlag(DefaultFlag.CREEPER_EXPLOSION, DefaultFlag.CREEPER_EXPLOSION.parseInput(worldGuard, player, "deny"));
        } catch (InvalidFlagFormat ignored) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Ocorreu um erro ao setar as proteções do seu terreno.");
        }

        final ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(protectedCuboidRegion);
        final LocalPlayer localPlayer = worldGuard.wrapPlayer(player);

        if (!applicableRegions.isOwnerOfAll(localPlayer) && !player.isOp() && !player.hasPermission("29Terrains.*")) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você está muito próximo de outro terreno!");
            regionManager.removeRegion(id);
            return;
        }

        final Terrain terrain = new Terrain(player.getName(), area, sizeX);
        double value = (sizeX * plugin.getConfig().getDouble("Settings.Price per block")) + terrain.getIPTUPrice();

        final PurchaseTerrainEvent purchaseTerrainEvent = new PurchaseTerrainEvent(player, terrain, regionManager, protectedCuboidRegion, player.getLocation(), value);
        plugin.callEvent(purchaseTerrainEvent);

        if (purchaseTerrainEvent.isCancelled()) {
            regionManager.removeRegion(id);
            return;
        }

        value = purchaseTerrainEvent.getValue();

        try {
            regionManager.save();
        } catch (StorageException exception) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Ocorre um erro ao tentar salvar seu terreno.");
        }

        if (!economy.has(player, value)) {
            double needed = value - economy.getBalance(player);

            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você precisa de mais <f>R$ " + NumberUtil.formatNumberSimple(needed, '.') + " <c>para comprar este Terreno!");
            regionManager.removeRegion(id);
            return;
        }

        economy.withdrawPlayer(player, value);
        terrainAPI.addTerrain(player.getName(), terrain);

        terrain.saveAsync();

        buildWall(player, sizeX);

        sendMessages(player,
                "",
                " <a>Terreno <f>" + area + " <a>comprado por <f>R$ " + NumberUtil.formatNumberSimple(value, '.') + "<a>.",
                " <a>Você precisa pagar mais <f>R$ " + NumberUtil.formatNumberSimple(terrain.getIPTUPrice(), '.') + " <a>no dia <f>" + TimeFormat.getTime(terrain.getExpiration()) + "<a>.",
                "",
                " <6>☣ <c>Caso você não pague, você não poderá mexer no seu terreno até pagar!",
                "");

        player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);
    }

    public void addFriend(Player owner, Player friend, String id) {
        if (!terrainAPI.isCorrectWorld(owner.getWorld())) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não é possivel gerênciar terrenos neste mundo.");
            return;
        }

        if (owner.getName().equalsIgnoreCase(friend.getName())) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você não pode adicionar você mesmo ao terreno.");
            return;
        }

        final RegionManager regionManager = worldGuard.getRegionManager(owner.getWorld());
        final ProtectedRegion protectedRegion = regionManager.getRegion(id);
        final String terrain = id.split("-")[id.split("-").length - 1];

        if (protectedRegion == null) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>O terreno <f>" + terrain + " <c>não foi encontrado.");
            return;
        }

        if (protectedRegion.getMembers().size() >= 10) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você só pode por <f>10 <c>amigos no terreno <f>" + terrain + "<c>.");
            return;
        }

        if (protectedRegion.getMembers().contains(friend.getName())) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <f>" + friend.getName() + " <c>já está adicionado neste terreno.");
            return;
        }

        protectedRegion.getMembers().addPlayer(friend.getName());

        sendMessages(owner,
                Sound.SUCCESSFUL_HIT,
                " <2>✔ <f>" + friend.getName() + " <a>adicionado ao terreno <f>" + terrain + "<a>.");

        sendMessages(friend,
                Sound.CLICK,
                " <2>✔ <a>Você foi adicioando no terreno de <f>" + owner.getName() + "<a>.");

        try {
            regionManager.save();
        } catch (StorageException exception) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Ocorreu um erro ao salvar o terreno.");
        }
    }

    public void delFriend(Player owner, String friend, String id) {
        if (!terrainAPI.isCorrectWorld(owner.getWorld())) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não é possivel gerênciar terrenos neste mundo.");
            return;
        }

        final RegionManager regionManager = worldGuard.getRegionManager(owner.getWorld());
        final ProtectedRegion protectedRegion = regionManager.getRegion(id);
        final String terrain = id.split("-")[id.split("-").length - 1];

        if (owner.getName().equalsIgnoreCase(friend)) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você não pode remover você mesmo do terreno.");
            return;
        }

        if (protectedRegion == null) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>O terreno <f>" + terrain + " <c>não foi encontrado.");
            return;
        }

        if (!protectedRegion.getMembers().contains(friend)) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <f>" + friend + " <c>não tem permissão no terreno <f> " + terrain + "<c>.");
            return;
        }

        protectedRegion.getMembers().removePlayer(friend);
        sendMessages(owner,
                Sound.SUCCESSFUL_HIT,
                " <2>✔ <f>" + friend + " <a>removido do terreno <f>" + terrain + "<a>.");

        final Player friendPlayer = plugin.getServer().getPlayer(friend);

        if (friendPlayer != null) {
            sendMessages(friendPlayer,
                    Sound.VILLAGER_NO,
                    " <6>☣ <f>" + owner.getName() + " <c>removeu você do terreno <f>" + terrain + "<c>.");
        }

        try {
            regionManager.save();
        } catch (StorageException exception) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Ocorreu um erro ao salvar o terreno.");
        }
    }

    public void sellTerrain(Player owner, Player buyer, String id, double price) {
        if (!terrainAPI.isCorrectWorld(owner.getWorld())) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não é possivel vender terrenos neste mundo.");
            return;
        }

        if (owner.getName().equalsIgnoreCase(buyer.getName())) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você não pode vender um terreno a sí mesmo.");
            return;
        }

        final String terrainName = id.split("-")[id.split("-").length - 1];

        Sale sale = terrainAPI.getSale(owner.getName(), buyer.getName());
        final Sale saleCheck = terrainAPI.getSaleWithTerrain(owner.getName(), terrainName);

        if (saleCheck != null) {
            if (saleCheck.getTerrain().equalsIgnoreCase(terrainName)) {
                sendMessages(owner,
                        Sound.VILLAGER_NO,
                        " <6>☣ <c>Você já está vendendo o terreno <f>" + saleCheck.getTerrain() + "<c>!");
                return;
            }
        }

        if (!terrainAPI.hasTerrain(terrainName, owner.getName())) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>O terreno <f>" + terrainName + " <c>não foi encontrado.");
            return;
        }

        if (terrainAPI.hasTerrain(terrainName, buyer.getName())) {
            sendMessages(owner, "");
            StringUtil.sendCenteredWithBreak(owner,
                    "&cPor motivos de incompatibilidade, você não pode vender um terreno para alguém que possui outro terreno com o mesmo nome!");
            sendMessages(owner, "");
            owner.playSound(owner.getLocation(), Sound.VILLAGER_NO, 1, 1);
            return;
        }

        if (price < 0 || price > 25000000) {
            sendMessages(owner, Sound.VILLAGER_NO,
                    " <6>☣ <c>O valor não pode ser menor que <f>0 <c>e maior que <f>25 milhões<c>.");
            return;
        }

        if (sale != null) {
            sendMessages(owner, Sound.VILLAGER_NO,
                    " <6>☣ <c>Você já enviou uma proposta de venda para <f>" + buyer.getName() + "<c>!");
            return;
        }

        if (cantPurchase(buyer, false)) {
            sendMessages(owner, Sound.VILLAGER_NO,
                    " <6>☣ <f>" + buyer.getName() + " <c>atingiu o limite máximo de terrenos.");
            return;
        }

        final RegionManager regionManager = worldGuard.getRegionManager(owner.getWorld());
        final ProtectedRegion protectedRegion = regionManager.getRegion(id);

        if (protectedRegion == null) {
            sendMessages(owner, Sound.VILLAGER_NO,
                    " <6>☣ <c>O terreno <f>" + terrainName + " <c>não foi encontrado.");
            return;
        }

        final ApplicableRegionSet set = regionManager.getApplicableRegions(protectedRegion);

        if (set.size() > 1) {
            sendMessages(owner, Sound.VILLAGER_NO,
                    " <6>☣ <c>Você não pode vender áreas com dois ou mais terrenos no local.");
            return;
        }

        if (!economy.has(buyer, price)) {
            double needed = price - economy.getBalance(buyer);

            sendMessages(owner, Sound.VILLAGER_NO,
                    " <6>☣ <f>" + buyer.getName() + " <c>precisa de mais <f>R$ " + NumberUtil.formatNumberSimple(needed, '.') + "<c>.");
            return;
        }

        if (!confirmations.containsKey(owner.getName())) {
            addCooldown(owner, buyer, terrainName, price);
            return;
        }

        if (!confirmations.get(owner.getName()).equalsIgnoreCase(buyer.getName())) {
            confirmations.remove(owner.getName());
            addCooldown(owner, buyer, terrainName, price);
            return;
        }

        confirmations.remove(owner.getName());

        sale = new Sale(owner.getName(), buyer.getName(), terrainName, regionManager, protectedRegion, price);
        sale.selfDestruct();

        plugin.getSaleList().add(sale);

        String infoMessage;

        final RegionManager regionManager1 = worldGuard.getRegionManager(buyer.getWorld());
        final ApplicableRegionSet set1 = regionManager1.getApplicableRegions(buyer.getLocation());

        if (set1.size() != 0) {

            String id1 = set1.iterator().next().getId();
            ProtectedRegion protectedRegion1 = regionManager1.getRegion(id1);

            String owner1 = "";
            String areaName = "";

            if (protectedRegion1 != null && set1.size() != 0) {
                owner1 = protectedRegion1.getOwners().toUserFriendlyString().replace("name:", "");
                areaName = id1.split("_")[id1.split("_").length - 1].replace(owner1 + "-", "");
            }

            if (areaName.equalsIgnoreCase(terrainName) && owner.getName().equalsIgnoreCase(owner1))
                infoMessage = " <2><l>É o terreno que você está em cima!";
            else infoMessage = " <4><l>Não é o terreno em que você está em cima!";

        } else infoMessage = " <4><l>Não é o terreno em que você está em cima!";

        final Terrain terrain = terrainAPI.getTerrain(terrainName, owner.getName());

        String overdue;

        if (terrain.isExpired()) overdue = "Vencido";
        else overdue = "Pago <7>(Expiração: " + TimeFormat.getTime(terrain.getExpiration()) + ")";

        sendMessages(buyer, Sound.CLICK,
                "",
                " " + owner.getName() + " <a>deseja vender o terreno <f>" + terrainName + " <a>para você!",
                "",
                " <a>Tamanho: <f>" + terrain.getSize() + "x" + terrain.getSize(),
                " <a>Preço: <f>R$ " + NumberUtil.formatNumberSimple(price, '.'),
                " <a>IPTU: <f>" + overdue,
                " <a>Você tem <f>1 minuto <a>para aceitar a proposta.",
                "",
                " <a>Para aceitar digite: <f>/terreno comprar " + owner.getName() + "<a>!",
                " <a>Para recusar digite: <f>/terreno recusar " + owner.getName() + "<a>!",
                "");

        StringUtil.sendCenteredWithBreak(buyer, TXT.parse(infoMessage));
        sendMessages(buyer, "");

        sendMessages(owner, Sound.CLICK,
                "",
                " <f>" + buyer.getName() + " <a>recebeu sua proposta.",
                " <a>Valor: <f>R$ " + NumberUtil.formatNumberSimple(price, '.') + "<a> no terreno <f>" + terrainName + "<a>.",
                " <f>" + buyer.getName() + " <a>tem <f>1 minuto <a>para aceitar a proposta.",
                "",
                " <a>Para cancelar digite: <f>/terreno cancelar " + buyer.getName() + "<a>!",
                "");
    }

    public void acceptTerrain(Player buyer, Player owner, String terrainName) {
        final Sale sale = terrainAPI.getSale(owner.getName(), buyer.getName());

        if (!terrainAPI.isCorrectWorld(buyer.getWorld())) {
            sendMessages(buyer,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não é possivel comprar terrenos neste mundo.");
            return;
        }

        if (owner.getName().equalsIgnoreCase(buyer.getName())) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você não pode comprar um terreno de sí mesmo.");
            return;
        }

        if (sale == null) {
            sendMessages(buyer,
                    Sound.VILLAGER_NO,
                    " <6>☣ <f>" + owner.getName() + " <c>não enviou propostas de terrenos para você!");
            return;
        }

        if (cantPurchase(buyer, false)) {
            sendMessages(buyer,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você atingiu o limite máximo de terrenos.");
            return;
        }

        if (!economy.has(buyer, sale.getPrice())) {
            double needed = sale.getPrice() - economy.getBalance(buyer);

            sendMessages(buyer,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você precisa de mais <f>R$" + NumberUtil.formatNumberSimple(needed, '.') + "<c>.");
            return;
        }

        if (terrainAPI.hasTerrain(terrainName, buyer.getName())) {
            sendMessages(buyer,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você já possui um terreno com esse nome!");
            return;
        }

        final RegionManager regionManager = sale.getRegionManager();
        final ProtectedRegion protectedRegion = sale.getRegion();

        if (protectedRegion == null) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>O terreno <f>" + sale.getTerrain() + " <c>não foi encontrado.");

            sendMessages(buyer,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>O terreno <f>" + sale.getTerrain() + " <c>de <f>" + owner.getName() + " <c>não foi encontrado.");
            return;
        }

        final String id = owner.getName().toLowerCase() + "-" + sale.getTerrain();
        final String id1 = buyer.getName().toLowerCase() + "-" + terrainName.toLowerCase();
        final ProtectedCuboidRegion protectedCuboidRegion = new ProtectedCuboidRegion(id1, protectedRegion.getMaximumPoint(), protectedRegion.getMinimumPoint());

        regionManager.addRegion(protectedCuboidRegion);
        protectedCuboidRegion.setPriority(100);

        final ProtectedRegion protectedRegion1 = regionManager.getRegion(id1);

        if (protectedRegion1 == null) {
            sendMessages(owner, " <6>☣ <c>Não foi possivel vender o seu terreno.");
            sendMessages(buyer, " <6>☣ <c>Não foi possivel criar o seu terreno.");

            confirmations.remove(owner.getName());

            owner.playSound(owner.getLocation(), Sound.VILLAGER_NO, 1, 1);
            buyer.playSound(buyer.getLocation(), Sound.VILLAGER_NO, 1, 1);
            return;
        } else protectedRegion1.getOwners().addPlayer(buyer.getName());

        final double upX = protectedCuboidRegion.getMaximumPoint().getBlockX() + 1;
        final double upZ = protectedCuboidRegion.getMaximumPoint().getBlockZ() + 1;

        final double lowerX = protectedCuboidRegion.getMinimumPoint().getBlockX();
        final double lowerZ = protectedCuboidRegion.getMinimumPoint().getBlockZ();

        final double x = lowerX + (upX - lowerX) / 2;
        final double z = lowerZ + (upZ - lowerZ) / 2;

        final World world = plugin.getServer().getWorld(plugin.getConfig().getString("Settings.World"));
        final Location location = new Location(world, x, 4, z);

        location.setYaw(buyer.getLocation().getYaw());
        location.setPitch(buyer.getLocation().getPitch());

        final SellTerrainEvent sellTerrainEvent = new SellTerrainEvent(owner, buyer, terrainAPI.getTerrain(sale.getTerrain(), sale.getOwner()), sale, sale.getPrice(), location);
        plugin.callEvent(sellTerrainEvent);

        if (sellTerrainEvent.isCancelled()) return;

        sale.setPrice(sellTerrainEvent.getValue());

        final RegionManager regionManager1 = worldGuard.getRegionManager(buyer.getWorld());

        if (regionManager1 == null) {
            sendMessages(owner, Sound.VILLAGER_NO, " <6>☣ <c>Não foi possivel vender o seu terreno.");
            sendMessages(buyer, Sound.VILLAGER_NO, " <6>☣ <c>Não foi possivel criar o seu terreno.");

            confirmations.remove(owner.getName());
            return;

        } else regionManager1.removeRegion(id);

        try {
            protectedCuboidRegion.setFlag(DefaultFlag.PVP, DefaultFlag.PVP.parseInput(worldGuard, buyer, "allow"));
            protectedCuboidRegion.setFlag(DefaultFlag.USE, DefaultFlag.USE.parseInput(worldGuard, buyer, "allow"));
            protectedCuboidRegion.setFlag(DefaultFlag.ENDER_BUILD, DefaultFlag.ENDER_BUILD.parseInput(worldGuard, buyer, "deny"));
            protectedCuboidRegion.setFlag(DefaultFlag.CREEPER_EXPLOSION, DefaultFlag.CREEPER_EXPLOSION.parseInput(worldGuard, buyer, "deny"));
        } catch (InvalidFlagFormat exception) {
            exception.printStackTrace();

            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não foi possivel vender o seu terreno.");
            sendMessages(buyer,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não foi possivel criar o seu terreno.");
            return;
        }

        try {
            regionManager.save();
        } catch (StorageException exception) {
            exception.printStackTrace();

            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Ocorreu um erro ao salvar o terreno de <f>" + buyer.getName() + "<c>, contate um STAFF!",
                    " <6>☣ <c>Transação cancelada.");
            sendMessages(buyer,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Ocorreu um erro ao salvar o seu terreno, contate um STAFF!",
                    " <6>☣ <c>Transação cancelada.");
            return;
        }

        economy.withdrawPlayer(buyer, sale.getPrice());
        economy.depositPlayer(owner, sale.getPrice());

        sale.updateConfig();
        sale.delete();

        confirmations.remove(owner.getName());

        sendMessages(owner,
                Sound.SUCCESSFUL_HIT,
                "",
                " <2>✔ <f>" + buyer.getName() + " <a>comprou seu terreno <f>" + sale.getTerrain() + " <a>por <f>R$ " + NumberUtil.formatNumberSimple(sale.getPrice(), '.') + "<a>.",
                "");

        sendMessages(buyer,
                Sound.SUCCESSFUL_HIT,
                "",
                " <2>✔ <a>Você comprou o terreno <f>" + sale.getTerrain() + " <a>de <f>" + sale.getOwner() + " <a>por <f>R$ " + NumberUtil.formatNumberSimple(sale.getPrice(), '.') + "<a>.",
                "");
    }

    public void rejectSale(Player buyer, Player owner) {
        final Sale sale = terrainAPI.getSale(owner.getName(), buyer.getName());

        if (sale == null) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não foi encontrada propostas de vendas com <f>" + buyer.getName() + "<c>!");
            return;
        }

        sale.delete();

        sendMessages(buyer,
                Sound.VILLAGER_YES,
                "",
                " <6>☣ <c>Você cancelou a compra do terreno <f>" + sale.getTerrain() + "<c>.",
                "");

        sendMessages(owner,
                Sound.VILLAGER_NO,
                "",
                " <6>☣ <f>" + buyer.getName() + " <c>cancelou a compra do terreno <f>" + sale.getTerrain() + "<c>.",
                "");
    }

    public void cancelSale(Player owner, String buyer) {
        final Sale sale = terrainAPI.getSale(owner.getName(), buyer);
        final Player buyerPlayer = plugin.getServer().getPlayer(buyer);

        if (sale == null) {
            sendMessages(owner,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não foi encontrada propostas de vendas com <f>" + buyer + "<c>!");
            return;
        }

        sale.delete();

        sendMessages(owner,
                Sound.VILLAGER_YES,
                "",
                " <6>☣ <c>Você cancelou a venda do terreno <f>" + sale.getTerrain() + "<c>.",
                "");

        if (buyerPlayer != null)
            sendMessages(buyerPlayer,
                    Sound.VILLAGER_NO,
                    "",
                    " <6>☣ <f>" + owner.getName() + " <c>cancelou a venda do terreno <f>" + sale.getTerrain() + "<c>.",
                    "");
    }

    public void terrainInfo(Player player) {
        final RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());
        final ApplicableRegionSet set = regionManager.getApplicableRegions(player.getLocation());

        if (set.size() == 0) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você não está em cima de nenhum terreno.");
            return;
        }

        final String id = set.iterator().next().getId();
        final ProtectedRegion protectedRegion = regionManager.getRegion(id);

        if (protectedRegion == null) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você não está em cima de nenhum terreno.");
            return;
        }

        if (protectedRegion.getOwners().toUserFriendlyString().isEmpty()) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você não está em cima de nenhum terreno.");
            return;
        }

        String owner = protectedRegion.getOwners().toUserFriendlyString().replace("name:", "");
        final String areaName = id.split("_")[id.split("_").length - 1].replace(owner.toLowerCase() + "-", "");

        final Terrain terrain = terrainAPI.getTerrain(areaName, owner);

        String size = "", iptu = "";
        if (terrain != null) {

            if (terrain.isExpired()) iptu = "<c>Vencido<7>.";
            else iptu = "Pago.";

            size = terrain.getSizeMessage();
        }

        boolean isMemberOrOwner = protectedRegion.isMember(worldGuard.wrapPlayer(player));

        String membersString = "";
        for (String member : protectedRegion.getMembers().getPlayers()) {
            member =  member.replace("name:", "");

            if (membersString.isEmpty()) membersString += member;
            else membersString += ", " + member;
        }

        if (!isMemberOrOwner && !player.isOp()) {
            sendMessages(player,
                    Sound.CLICK,
                    " <2>✔ <a>Esse terreno pertence a <f>" + owner + "<a>.");
            return;
        }

        String terrainMessage;

        if (player.getName().equalsIgnoreCase(owner)) terrainMessage = " <2>✔ <a>Você é o dono deste terreno!";
        else if (isMemberOrOwner) terrainMessage = "<2>✔ <a>Você é membro deste terreno!";
        else terrainMessage = " <c>✖ <e>Você não é o dono deste terreno!";

        sendMessages(player,
                "",
                terrainMessage);

        if (!membersString.isEmpty()) {

            if (membersString.contains(player.getName().toLowerCase()))  {
                membersString = membersString.replace(player.getName().toLowerCase(), "você");
            }

        } else membersString = "Nenhum";

        sendMessages(player,
                Sound.CLICK,
                "",
                " <a>Terreno: <f>" + StringUtils.capitalize(areaName) + " <7>(" + size + ")",
                " <a>Dono: <f>" + StringUtils.capitalize(owner),
                " <a>Membros: <f>" + WordUtils.capitalize(membersString) + ".",
                " <a>IPTU: <f>" + iptu,
                "");
    }

    public void payIPTU(Player player) {
        final RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());
        final ApplicableRegionSet set = regionManager.getApplicableRegions(player.getLocation());

        if (set.size() == 0) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você não está em cima de nenhum terreno.");
            return;
        }

        final String id = set.iterator().next().getId();
        final ProtectedRegion protectedRegion = regionManager.getRegion(id);

        if (protectedRegion == null) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você não está em cima de nenhum terreno.");
            return;
        }

        final String owner = protectedRegion.getOwners().toUserFriendlyString().replace("name:", "");
        final String areaName = id.split("_")[id.split("_").length - 1].replace(owner.toLowerCase() + "-", "");
        final Terrain terrain = terrainAPI.getTerrain(areaName, owner);

        boolean isMemberOrOwner = protectedRegion.isMember(worldGuard.wrapPlayer(player));

        if (terrain == null) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>O terreno <f>" + areaName + " <c>não foi encontrado.");
            return;
        }

        if (!isMemberOrOwner && !player.hasPermission("29Terrains.*")) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Apenas o dono, membro ou staff pode pagar o <f>IPTU<c>!");
            return;
        }

        if (!terrain.isBlocked()) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    "",
                    " <2>✔ <a>Este terreno não está com o <f>IPTU <a>vencido!",
                    " <2>✔ <a>Próximo IPTU: <f>" + TimeFormat.getTime(terrain.getExpiration()) + "<a>.",
                    "");
            return;
        }

        if (!economy.has(player, terrain.getIPTUPrice())) {
            double needed = terrain.getIPTUPrice() - economy.getBalance(player);

            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Você precisa de mais <f>R$ " + NumberUtil.formatNumberSimple(needed, '.') + " <c>para pagar o IPTU!");
            return;
        }

        economy.withdrawPlayer(player, terrain.getIPTUPrice());
        terrain.updateExpiration();

        sendMessages(player,
                Sound.SUCCESSFUL_HIT,
                " <2>✔ <a>Você pagou o <f>IPTU <a>do terreno <f>" + terrain.getName() + "<a> por <f>R$" + NumberUtil.formatNumberSimple(terrain.getIPTUPrice(), '.') + "!");

    }

    private void buildWall(Player player, int size) {
        final Location location = player.getLocation();
        final World world = player.getWorld();

        final Material fence = Material.getMaterial(plugin.getConfig().getString("Settings.Fence"));

        final int initialPositionX = location.getBlockX() - size / 2;
        final int initialPositionZ = location.getBlockZ() - size / 2;
        final int finalPositionX = location.getBlockX() + size / 2;
        final int finalPositionZ = location.getBlockZ() + size / 2;

        if (plugin.getConfig().getBoolean("Settings.Del Tree")) {

            for (int y = 20; y < 90; y++) {
                for (int x = initialPositionX; x < finalPositionX; x++) {
                    for (int z = initialPositionZ; z < finalPositionZ; z++) {
                        Block block = new Location(world, x, y, z).getBlock();

                        if (block.getType() == Material.LEAVES || block.getType() == Material.LOG) {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }

        final boolean lineUp = plugin.getConfig().getBoolean("Settings.Line up");

        if (!lineUp) {

            for (int x = initialPositionX; x < finalPositionX; ++x) {
                Block block = world.getHighestBlockAt(x, initialPositionZ);
                block.setType(fence);
            }

            for (int x = initialPositionX; x <= finalPositionX; ++x) {
                Block block = world.getHighestBlockAt(x, finalPositionZ);
                block.setType(fence);
            }

            for (int z = initialPositionZ; z < finalPositionZ; ++z) {
                Block block = world.getHighestBlockAt(initialPositionX, z);
                block.setType(fence);
            }

            for (int z = initialPositionZ; z <= finalPositionZ; ++z) {
                Block block = world.getHighestBlockAt(finalPositionX, z);
                block.setType(fence);
            }

        } else {

            for (int x = initialPositionX; x < finalPositionX; ++x) {
                Block block = world.getBlockAt(x, location.getBlockY(), initialPositionZ);
                block.setType(fence);
            }

            for (int x = initialPositionX; x <= finalPositionX; ++x) {
                Block block = world.getBlockAt(x, location.getBlockY(), finalPositionZ);
                block.setType(fence);
            }

            for (int z = initialPositionZ; z < finalPositionZ; ++z) {
                Block block = world.getBlockAt(initialPositionX, location.getBlockY(), z);
                block.setType(fence);
            }

            for (int z = initialPositionZ; z <= finalPositionZ; ++z) {
                Block block = world.getBlockAt(finalPositionX, location.getBlockY(), z);
                block.setType(fence);
            }
        }
    }

    public void delTerrain(Player player, String area, String owner) {
        final Location location = player.getLocation();
        final World world = location.getWorld();

        if (!terrainAPI.isCorrectWorld(world)) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não é possivel gerênciar terrenos neste mundo.");
            return;
        }

        if (!terrainAPI.hasTerrain(area, owner)) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>O terreno <f>" + area + " <c>não foi encontrado.");
            return;
        }

        final RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());
        final ProtectedRegion protectedRegion = regionManager.getRegion(owner.toLowerCase() + "-" + area);

        final Terrain terrain = terrainAPI.getTerrain(area, owner);

        if (protectedRegion == null) {
            terrain.deleteAsync();
            sendMessages(player,
                    Sound.LEVEL_UP,
                    " <2>✔ <a>Você deletou o terreno <f>" + area + "<a> <a>de <f>" + owner + "!");
            return;
        }

        final double upX = protectedRegion.getMaximumPoint().getBlockX() + 1;
        final double upZ = protectedRegion.getMaximumPoint().getBlockZ() + 1;

        final double lowerX = protectedRegion.getMinimumPoint().getBlockX();
        final double lowerZ = protectedRegion.getMinimumPoint().getBlockZ();

        final double xL = lowerX + (upX - lowerX) / 2;
        final double zL = lowerZ + (upZ - lowerZ) / 2;

        final Location terrainLocation = new Location(world, xL, 4, zL);

        terrainLocation.setYaw(location.getYaw());
        terrainLocation.setPitch(location.getPitch());

        final DeleteTerrainEvent deleteTerrainEvent = new DeleteTerrainEvent(player, terrain, regionManager, protectedRegion, terrainLocation);
        plugin.callEvent(deleteTerrainEvent);

        if (deleteTerrainEvent.isCancelled()) return;

        final int xMin = protectedRegion.getMinimumPoint().getBlockX();
        final int xMax = protectedRegion.getMaximumPoint().getBlockX();
        final int zMin = protectedRegion.getMinimumPoint().getBlockZ();
        final int zMax = protectedRegion.getMaximumPoint().getBlockZ();

        final Material fence = Material.getMaterial(plugin.getConfig().getString("Settings.Fence"));

        for (int y = 0; y < 256; ++y) {
            for (int x = xMin; x < xMax; ++x) {
                Block block = world.getBlockAt(x, y, zMin);

                if (block.getType() == fence) block.setType(Material.AIR);
            }

            for (int x = xMin; x <= xMax; ++x) {
                Block block = world.getBlockAt(x, y, zMax);

                if (block.getType() == fence) block.setType(Material.AIR);
            }

            for (int z = zMin; z < zMax; ++z) {
                Block block = world.getBlockAt(xMin, y, z);

                if (block.getType() == fence) block.setType(Material.AIR);
            }

            for (int z = zMin; z <= zMax; ++z) {
                Block block = world.getBlockAt(xMax, y, z);

                if (block.getType() == fence) block.setType(Material.AIR);
            }
        }

        if (terrain != null) terrain.deleteAsync();

        regionManager.removeRegion(owner.toLowerCase() + "-" + area);

        try {
            regionManager.save();
        } catch (StorageException exception) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Ocorreu um erro ao salvar o terreno deletado.");
            return;
        }

        sendMessages(player, Sound.LEVEL_UP, " <2>✔ <a>Você deletou o terreno <f>" + area + "<a> <a>de <f>" + owner + "!");
    }

    public void delTerrain(Player player, String area) {
        final Location location = player.getLocation();
        final World world = location.getWorld();

        if (!terrainAPI.isCorrectWorld(world)) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Não é possivel gerênciar terrenos neste mundo.");
            return;
        }

        final RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());
        final String id = player.getName().toLowerCase() + "-" + area;

        if (!terrainAPI.hasTerrain(area, player.getName())) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>O terreno <f>" + area + " <c>não foi encontrado.");
            return;
        }

        final Terrain terrain = terrainAPI.getTerrain(area, player.getName());
        final ProtectedRegion protectedRegion = regionManager.getRegion(id);

        if (protectedRegion == null) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>O terreno <f>" + area + " <c>não foi encontrado.");
            return;
        }

        final double upX = protectedRegion.getMaximumPoint().getBlockX() + 1;
        final double upZ = protectedRegion.getMaximumPoint().getBlockZ() + 1;

        final double lowerX = protectedRegion.getMinimumPoint().getBlockX();
        final double lowerZ = protectedRegion.getMinimumPoint().getBlockZ();

        final double xL = lowerX + (upX - lowerX) / 2;
        final double zL = lowerZ + (upZ - lowerZ) / 2;

        final Location terrainLocation = new Location(world, xL, 4, zL);

        terrainLocation.setYaw(location.getYaw());
        terrainLocation.setPitch(location.getPitch());

        final DeleteTerrainEvent deleteTerrainEvent = new DeleteTerrainEvent(player, terrain, regionManager, protectedRegion, terrainLocation);
        plugin.callEvent(deleteTerrainEvent);

        if (deleteTerrainEvent.isCancelled()) return;

        final int xMin = protectedRegion.getMinimumPoint().getBlockX();
        final int xMax = protectedRegion.getMaximumPoint().getBlockX();
        final int zMin = protectedRegion.getMinimumPoint().getBlockZ();
        final int zMax = protectedRegion.getMaximumPoint().getBlockZ();

        final Material fence = Material.getMaterial(plugin.getConfig().getString("Settings.Fence"));

        for (int y = 0; y < 256; ++y) {
            for (int x = xMin; x < xMax; ++x) {
                Block block = world.getBlockAt(x, y, zMin);

                if (block.getType() == fence) block.setType(Material.AIR);
            }

            for (int x = xMin; x <= xMax; ++x) {
                Block block = world.getBlockAt(x, y, zMax);

                if (block.getType() == fence) block.setType(Material.AIR);
            }

            for (int z = zMin; z < zMax; ++z) {
                Block block = world.getBlockAt(xMin, y, z);

                if (block.getType() == fence) block.setType(Material.AIR);
            }

            for (int z = zMin; z <= zMax; ++z) {
                Block block = world.getBlockAt(xMax, y, z);

                if (block.getType() == fence) block.setType(Material.AIR);
            }
        }

        if (terrain != null) terrain.deleteAsync();

        regionManager.removeRegion(id);

        try {
            regionManager.save();
        } catch (StorageException exception) {
            sendMessages(player,
                    Sound.VILLAGER_NO,
                    " <6>☣ <c>Ocorreu um erro ao salvar o terreno deletado.");
            return;
        }

        sendMessages(player,
                Sound.LEVEL_UP,
                " <2>✔ <a>Você deletou o terreno <f>" + area + "<a>!");
    }

    private void addCooldown(Player owner, Player buyer, String terrain, double price) {
        confirmations.put(owner.getName(), buyer.getName());

        sendMessages(owner,
                Sound.SUCCESSFUL_HIT,
                " <a>Deseja vender o terreno <f>" + terrain + " <a>por <f>R$ " + NumberUtil.formatNumberSimple(price, '.') + "<a> para <f>" + buyer.getName() + "<a>?",
                " <a>Para confirmar digite o comando novamente, ou espere 15 segundos para cancelar!");

        new BukkitRunnable() {
            @Override
            public void run() {

                if (confirmations.containsKey(owner.getName())) {
                    sendMessages(owner,
                            Sound.VILLAGER_NO,
                            " <6>☣ <c>Você não digitou o comando novamente e a venda foi cancelada!");

                    confirmations.remove(owner.getName());
                }

            }
        }.runTaskLater(plugin, 300);
    }

    void sendMessages(Player player, String... texts) {
        TXT.sendMessages(player, texts);
    }

    void sendMessages(Player player, Sound sound, String... texts) {
        TXT.sendMessages(player, texts);

        player.playSound(player.getLocation(), sound, 1, 1);
    }
}
