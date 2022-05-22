package dev.cromo29.terrains.commands;

import dev.cromo29.durkcore.api.DurkCommand;
import dev.cromo29.terrains.TerrainPlugin;
import org.bukkit.entity.Player;

import java.util.List;

public class TerrainCMD extends DurkCommand {

    private final TerrainPlugin plugin;

    public TerrainCMD(TerrainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void perform() {

        if (isArgsLength(2)) {

            if (isArgAtIgnoreCase(0, "cancelar", "cancel")) {

                plugin.getTerrainManager().cancelSale(asPlayer(), argAt(1));

            } else if (isArgAtIgnoreCase(0, "recusar", "deny")) {

                Player owner = getPlayer(argAt(0));

                if (owner == null) {
                    warnPlayerOffline(argAt(0));
                    return;
                }

                plugin.getTerrainManager().rejectSale(owner, asPlayer());

            } else sendHelp();

        } else if (isArgsLength(3)) {

            if (isArgAtIgnoreCase(0, "comprar", "buy")) {

                Player target = getPlayer(argAt(1));

                if (target == null) {
                    warnPlayerOffline(argAt(1));
                    return;
                }

                plugin.getTerrainManager().acceptTerrain(asPlayer(), target, argAt(0));

            } else sendHelp();

        } else if (isArgsLength(4)) {

            if (isArgAtIgnoreCase(0, "vender")) {

                String terrain = argAt(1);
                Player buyer = getPlayer(argAt(2));
                double price = getDouble(argAt(3));

                if (buyer == null) {

                    warnPlayerOffline(argAt(2));
                    return;

                } else if (!isValidDouble(argAt(3))) {

                    warnNotValidNumber(argAt(3));
                    return;

                }

               final String id = asPlayer().getName().toLowerCase() + "-" + terrain;

                plugin.getTerrainManager().sellTerrain(asPlayer(), buyer, id, price);

            } else sendHelp();

        } else sendHelp();
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
        return "terreno";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    private void sendHelp() {
        sendMessages("",
                "<b>- <r>/" + getUsedCommand() + " vender <terreno> <comprador> <valor> <e>- <7>Criar uma proposta.",
                "<b>- <r>/" + getUsedCommand() + " comprar <vendedor> <novo nome> <e>- <7>Aceitar proposta.",
                "<b>- <r>/" + getUsedCommand() + " recusar <vendedor> <e>- <7>Recusar proposta.",
                "<b>- <r>/" + getUsedCommand() + " cancelar <comprador> <e>- <7>Cancelar proposta.",
                "");
    }
}
