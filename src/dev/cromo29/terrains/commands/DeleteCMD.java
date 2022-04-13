package dev.cromo29.terrains.commands;

import dev.cromo29.durkcore.API.DurkCommand;
import dev.cromo29.terrains.TerrainPlugin;

import java.util.List;

public class DeleteCMD extends DurkCommand {

    private final TerrainPlugin plugin;

    public DeleteCMD(TerrainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void perform() {

        if (isArgsLength(2)) {

            if (!hasPermission("29Terrains.*")) {
                warnNoPermission();
                return;
            }

            String area = argAt(0);
            String user = argAt(1);

            plugin.getTerrainManager().delTerrain(asPlayer(), area, user);

        } else if (isArgsLength(1)) {

            plugin.getTerrainManager().delTerrain(asPlayer(), argAt(0));

        } else {

            sendMessage("<b>- <r>/" + getUsedCommand() + " <nome> <e>- <7>Deletar um terreno.");

            if (hasPermission("29Terrains.*"))
                sendMessage("<b>- <r>/" + getUsedCommand() + " <nome> <dono> <e>- <7>Deletar terreno de alguém.");

        }
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
        return "deletararea";
    }

    @Override
    public List<String> getAliases() {
        return getList("delarea");
    }

}
