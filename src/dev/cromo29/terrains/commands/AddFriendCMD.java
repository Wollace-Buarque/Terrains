package dev.cromo29.terrains.commands;

import dev.cromo29.durkcore.API.DurkCommand;
import dev.cromo29.terrains.TerrainPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public class AddFriendCMD extends DurkCommand {

    private final TerrainPlugin plugin;

    public AddFriendCMD(TerrainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void perform() {
        if (isArgsLength(2)) {

            Player target = getPlayer(argAt(0));

            if (target == null) {
                warnPlayerOffline(argAt(0));
                playSound(asPlayer(), Sound.VILLAGER_NO, 1, 1);
                return;
            }

            plugin.getTerrainManager().addFriend(asPlayer(), target, asPlayer().getName().toLowerCase() + "-" + argAt(1));

        } else sendMessage("<b>- <r>/" + getUsedCommand() + " <nome> <area> <e>- <7>Adicionar alguém no terreno.");
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
        return "adicionaramigo";
    }

    @Override
    public List<String> getAliases() {
        return getList( "addamigo");
    }
}
