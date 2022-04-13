package dev.cromo29.terrains.events;

import dev.cromo29.durkcore.Util.TXT;
import dev.cromo29.terrains.TerrainPlugin;
import dev.cromo29.terrains.object.Terrain;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinEvent implements Listener {

    private final TerrainPlugin plugin;

    public JoinEvent(TerrainPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        handle(event.getPlayer());

    }

    private void handle(Player player) {
        int terrainsSize = (int) plugin.getTerrainAPI().getTerrains(player.getName()).stream()
                .filter(Terrain::isBlocked).count();

        if (terrainsSize == 0) return;

        TXT.runLater(plugin, 40, () -> {
            TXT.sendMessages(player,
                    "",
                    " <6>☣ <c>Você possui <f>" + terrainsSize + " <c>terreno(s) bloqueado(s)!",
                    "");

            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1, 1);
        });
    }
}
