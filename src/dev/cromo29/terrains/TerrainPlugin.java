package dev.cromo29.terrains;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import dev.cromo29.durkcore.api.DurkPlugin;
import dev.cromo29.durkcore.specificutils.NumberUtil;
import dev.cromo29.durkcore.util.GsonManager;
import dev.cromo29.durkcore.util.TXT;
import dev.cromo29.durkcore.util.TimeFormat;
import dev.cromo29.durkcore.util.VaultAPI;
import dev.cromo29.terrains.api.GuiAPI;
import dev.cromo29.terrains.api.TerrainAPI;
import dev.cromo29.terrains.events.*;
import dev.cromo29.terrains.managers.TerrainManager;
import dev.cromo29.terrains.commands.*;
import dev.cromo29.terrains.object.Terrain;
import dev.cromo29.terrains.object.Sale;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public class TerrainPlugin extends DurkPlugin {

    private Map<String, List<Terrain>> terrainMap;
    private List<Sale> saleList;
    private GsonManager data;

    private TerrainAPI terrainAPI;
    private TerrainManager terrainManager;
    private GuiAPI guiAPI;

    @Override
    public void onStart() {
        terrainMap = new HashMap<>();
        saleList = new ArrayList<>();

        String path = getDataFolder().getPath() + File.separator + "storage";
        data = new GsonManager(path, "terrains.json").prepareGson();

        saveDefaultConfig();

        loadManagers();
        terrainAPI.loadTerrains();

        registerCommands(new PurchaseCMD(this),
                new DeleteCMD(this),
                new AddFriendCMD(this),
                new DeleteFriendCMD(this),
                new InfoCMD(this),
                new AreasCMD(this),
                new IptuCMD(this),
                new TerrainCMD(this));

        setListeners(new BlockedTerrainEvent(this), new BreakPlatationEvent(this),
                new DelRegionEvent(this), new JoinEvent(this), new SetHomeEvent(this));

        checkTerrains();
    }


    public static TerrainPlugin get() {
        return getPlugin(TerrainPlugin.class);
    }


    public GsonManager getData() {
        return data;
    }


    public Map<String, List<Terrain>> getTerrainMap() {
        return terrainMap;
    }

    public List<Sale> getSaleList() {
        return saleList;
    }


    public TerrainManager getTerrainManager() {
        return terrainManager;
    }


    public TerrainAPI getTerrainAPI() {
        return terrainAPI;
    }

    public GuiAPI getGuiAPI() {
        return guiAPI;
    }


    public WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getPlugin("WorldGuard");

        if (!(plugin instanceof WorldGuardPlugin)) return null;

        return (WorldGuardPlugin) plugin;
    }


    private void loadManagers() {
        this.terrainAPI = new TerrainAPI(this);
        this.terrainManager = new TerrainManager(this);
        this.guiAPI = new GuiAPI(this);
    }


    private void checkTerrains() {
        long verifyTime = getConfig().getInt("Settings.IPTU check") * 60 * 20L;

        if (verifyTime <= 0) verifyTime = 1200;

        TXT.runAsynchronously(this, 200, verifyTime, () -> {

            if (terrainMap.isEmpty()) return;

            Iterator<Terrain> terrainIterator = terrainAPI.getTerrains().iterator();

            while (terrainIterator.hasNext()) {
                Terrain terrain = terrainIterator.next();

                if (!terrain.isExpired()) continue;

                if (terrain.isAutomatic()) {
                    OfflinePlayer off = getServer().getOfflinePlayer(terrain.getOwner());

                    if (VaultAPI.getEconomy().has(off, terrain.getIPTUPrice())) {
                        VaultAPI.getEconomy().withdrawPlayer(off, terrain.getIPTUPrice());

                        terrain.updateExpiration();

                        if (off.isOnline()) {
                            TXT.sendMessages(off.getPlayer(),
                                    "",
                                    " <2>✔ <a>IPTU do terreno <f>" + terrain.getName() + " <a>pago automáticamente.",
                                    " <2>✔ <a>Valor: <f>R$ " + NumberUtil.formatNumberSimple(terrain.getIPTUPrice(), '.'),
                                    " <2>✔ <a>Expiração: <f>" + TimeFormat.getTime(terrain.getExpiration()),
                                    "");

                            off.getPlayer().playSound(off.getPlayer().getLocation(), Sound.LEVEL_UP, 1, 1);
                        }

                        continue;
                    }
                }

                if (!terrain.isBlocked()) {
                    terrain.block();

                    Player owner = getServer().getPlayer(terrain.getOwner());

                    if (owner != null) {
                        TXT.sendMessages(owner,
                                "",
                                " <6>☣ <c>O <f>IPTU <c>do terreno <f>" + terrain.getName() + " <c>expirou!",
                                " <6>☣ <c>Você só poderá mexer no terreno após pagar o <f>IPTU <c>vencido!",
                                " <6>☣ <c>Para pagar digite <f>/terrenos <c>e procure por <f>IPTU<c>!",
                                "");
                    }
                }
            }
        });
    }
}
