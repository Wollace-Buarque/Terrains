package dev.cromo29.terrains.commands;

import dev.cromo29.durkcore.API.DurkCommand;
import dev.cromo29.terrains.TerrainPlugin;

import java.util.List;

public class IptuCMD extends DurkCommand {

    private final TerrainPlugin plugin;

    public IptuCMD(TerrainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void perform() {
        plugin.getTerrainManager().payIPTU(asPlayer());
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
        return "iptu";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }
}
