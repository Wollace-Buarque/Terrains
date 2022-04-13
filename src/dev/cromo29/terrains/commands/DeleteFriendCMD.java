package dev.cromo29.terrains.commands;

import dev.cromo29.durkcore.API.DurkCommand;
import dev.cromo29.terrains.TerrainPlugin;

import java.util.List;

public class DeleteFriendCMD extends DurkCommand {

    private final TerrainPlugin plugin;

    public DeleteFriendCMD(TerrainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void perform() {
        if (isArgsLength(2)) {

            plugin.getTerrainManager().delFriend(asPlayer(), argAt(0), asPlayer().getName().toLowerCase() + "-" + argAt(1));

        } else sendMessage("<b>- <r>/" + getUsedCommand() + " <nome> <area> <e>- <7>Remover alguém do terreno.");
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
        return "deletaramigo";
    }

    @Override
    public List<String> getAliases() {
        return getList("delamigo");
    }

}
