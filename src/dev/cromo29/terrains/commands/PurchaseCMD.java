package dev.cromo29.terrains.commands;

import dev.cromo29.durkcore.API.DurkCommand;
import dev.cromo29.terrains.managers.TerrainManager;
import dev.cromo29.terrains.TerrainPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class PurchaseCMD extends DurkCommand {

    private final TerrainManager terrainManager;
    private final FileConfiguration configuration;

    public PurchaseCMD(TerrainPlugin plugin) {
        this.terrainManager = plugin.getTerrainManager();
        this.configuration = plugin.getConfig();
    }

    @Override
    public void perform() {

        if (isArgsLength(2)) {

            if (terrainManager.cantPurchase(asPlayer(), true)) return;

            String terrain = argAt(0);

            if (!isValidInt(argAt(1))) {
                warnNotValidNumber(argAt(1));
                return;
            }

            int size = getInt(argAt(1));

            if (size < 10 || size > configuration.getInt("Settings.Max length")) {
                sendMessage(" <6>☣ <c>O tamanho precisa ter no minimo <f>10 <c> e no máximo <f>%s<c>.", configuration.getInt("Settings.Max length"));
                return;
            }

            if (size > 50 && !hasPermission("29Terrains.Plus") && !hasPermission("29Terrains.*")) {
                sendMessage(" <6>☣ <c>Você não pode comprar terrenos a cima de <f>50x50<c>!");
                return;
            }

            if (!isEven(size)) {
                sendMessage(" <6>☣ <c>Você só pode colocar números pares.");
                return;
            }

            if (terrain.length() > 12) {
                sendMessage(" <6>☣ <c>O nome não pode passar de <f>12 <c>caracteres.");
                return;
            }

            if (!terrain.matches("^[a-zA-Z0-9_]*$")) {
                sendMessage(" <6>☣ <c>Você não pode por caracteres especiais e acentos.");
                return;
            }

            terrainManager.purchase(asPlayer(), size, size, terrain);

        } else sendMessage("<b>- <r>/" + getUsedCommand() + " <nome> <tamanho> <e>- <7>Comprar um terreno.");
    }

    @Override
    public boolean canConsolePerform() {
        return false;
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public String getCommand() {
        return "comprar";
    }

    @Override
    public List<String> getAliases() {
        return getList("buy");
    }

    private boolean isEven(double num) {
        return ((num % 2) == 0);
    }
}
