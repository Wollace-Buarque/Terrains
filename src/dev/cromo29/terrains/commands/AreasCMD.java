package dev.cromo29.terrains.commands;

import dev.cromo29.durkcore.api.DurkCommand;
import dev.cromo29.terrains.api.GuiAPI;
import dev.cromo29.terrains.TerrainPlugin;

import java.util.List;

public class AreasCMD extends DurkCommand {

    private final TerrainPlugin plugin;
    private final GuiAPI guiAPI;

    public AreasCMD(TerrainPlugin plugin) {
        this.plugin = plugin;
        this.guiAPI = plugin.getGuiAPI();
    }

    @Override
    public void perform() {
        if (!isArgsLength(1) || !isArgAtIgnoreCase(0, "reload", "recarregar") || !hasPermission("29Terrains.*")) {
            guiAPI.openAreas(asPlayer(), true);
            return;
        }

        plugin.reloadConfig();
        plugin.getData().reload();

        sendMessage(" <2>✔ <a>Configuração recarregada com sucesso.");

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
        return "areas";
    }

    @Override
    public List<String> getAliases() {
        return getList("terrenos");
    }
}
