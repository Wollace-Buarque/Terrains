package dev.cromo29.terrains.api;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.cromo29.durkcore.Inventory.Confirmation;
import dev.cromo29.durkcore.Inventory.Inv;
import dev.cromo29.durkcore.SpecificUtils.LocationUtil;
import dev.cromo29.durkcore.SpecificUtils.NumberUtil;
import dev.cromo29.durkcore.Util.*;
import dev.cromo29.terrains.managers.TerrainManager;
import dev.cromo29.terrains.object.Terrain;
import dev.cromo29.terrains.TerrainPlugin;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GuiAPI {

    private final TerrainPlugin plugin;
    private final TerrainAPI terrainAPI;
    private final TerrainManager terrainManager;

    private final List<String> cooldowns;
    private final WorldGuardPlugin worldGuard;
    private final Economy economy;

    public GuiAPI(TerrainPlugin plugin) {
        this.plugin = plugin;
        this.terrainAPI = plugin.getTerrainAPI();
        this.terrainManager = plugin.getTerrainManager();

        this.cooldowns = new ArrayList<>();
        this.worldGuard = plugin.getWorldGuard();
        this.economy = VaultAPI.getEconomy();
    }

    public void openAreas(Player player, boolean warn) {
        final Inv inv = new Inv(54, "Suas areas:");
        inv.setIgnorePlayerInventoryClick(true, true);

        final List<Terrain> terrains = terrainAPI.getTerrains(player.getName());

        if (!terrains.isEmpty()) {

            long paidIPTUs = terrains
                    .stream()
                    .filter(terrain -> !terrain.isExpired())
                    .count();

            for (Terrain terrain : terrains) {
                String iptuMessage;

                if (terrain.isExpired()) iptuMessage = " <7>IPTU: <c>Vencido <7>(R$ " + NumberUtil.formatNumberSimple(terrain.getIPTUPrice(), '.') + ") ";
                else iptuMessage = " <7>Próximo IPTU: <f>" + TimeFormat.getTime(terrain.getExpiration()) + " <7>(R$ " + NumberUtil.formatNumberSimple(terrain.getIPTUPrice(), '.') + ") ";

                inv.setInMiddle(new MakeItem("Vinderguy")
                        .of("37410c07bfbb4145004bf918c8d6301bd97ce13270ce1f221d9aabee1afd52a3")
                        .setName("<e>" + StringUtils.capitalize(terrain.getName()))
                        .addLoreList("",
                                " <7>Clique aqui para gerênciar esse terreno. ",
                                iptuMessage,
                                "")
                        .build(), event -> {

                    areaMenu(player, terrain.getName());

                    player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
                });

                boolean hasPerm = false;
                int toReturn = 0;

                if (player.hasPermission("29Terrains.*") || player.isOp()) {

                    inv.setItem(53, new MakeItem(player.getName())
                            .of("da900339bb971f028afe6724a85219ba2339918ba49c119d2fb871e47ac99b39")
                            .setName("<e>Casas")
                            .addLoreList("",
                                    " <7>Casas compradas: <f>" + terrains.size() + "<7>/<f>infinitas <7>casas atualmente. ",
                                    " <7>IPTUs pagos: <f>" + paidIPTUs + "<7>/<f>" + terrains.size() + "<7>.",
                                    "")
                            .build());

                    hasPerm = true;

                } else {

                    int index = 1;
                    while (index < 100) {

                        if (player.hasPermission("29Terrains." + index)) {

                            inv.setItem(53, new MakeItem(player.getName())
                                    .of("da900339bb971f028afe6724a85219ba2339918ba49c119d2fb871e47ac99b39")
                                    .setName("<e>Casas")
                                    .addLoreList("",
                                            " <7>Casas compradas: <f>" + terrains.size() + "<7>/<f>" + index + " <7>casas atualmente. ",
                                            " <7>IPTUs pagos: <f>" + paidIPTUs + "<7>/<f>" + terrains.size() + "<7>.",
                                            "")
                                    .build());

                            toReturn = index;
                            hasPerm = true;
                            break;

                        } else index++;

                    }

                }

                if (!player.hasPermission("29Terrains.*") && !player.isOp()) {
                    int limit = (hasPerm ? toReturn : plugin.getConfig().getInt("Settings.Max area"));

                    inv.setItem(53, new MakeItem(player.getName())
                            .of("da900339bb971f028afe6724a85219ba2339918ba49c119d2fb871e47ac99b39")
                            .setName("<e>Casas")
                            .addLoreList("",
                                    " <7>Casas compradas: <f>" + terrains.size() + "<7>/<f>" + limit + " <7>casas atualmente. ",
                                    " <7>IPTUs pagos: <f>" + paidIPTUs + "<7>/<f>" + terrains.size() + "<7>.",
                                    "")
                            .build());
                }
            }

        } else {

            inv.setItem(22, new MakeItem(Material.WEB)
                    .setName("<c>Você não tem nenhum terreno.")
                    .build());

            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1, 1);

        }

        inv.setItem(49, new MakeItem(Material.ARROW)
                .setName(" <e>Sair")
                .addLoreList(
                        "",
                        " <7>Clique aqui para sair. ",
                        "")
                .build(), event -> {

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

        });

        if (warn) sendMessage(player, Sound.CHEST_OPEN, " <2>✔ <a>Abrindo lista de terrenos...");

        inv.open(player);
    }

    private void areaMenu(Player player, String terrain) {
        final Inv inv = new Inv(54, "Configurações do terreno:");
        inv.setIgnorePlayerInventoryClick(true, true);

        final String lowerName = player.getName().toLowerCase();

                                            // worldGuard.getRegionManager(player.getWorld());
        final RegionManager regionManager = worldGuard.getRegionManager(plugin.getServer().getWorld(plugin.getConfig().getString("Settings.World")));
        final ProtectedRegion protectedRegion = regionManager.getRegion(lowerName + "-" + terrain);

        if (protectedRegion == null) {
            sendMessage(player,
                    "",
                    "<c>Não foi possível encontrar este terreno.",
                    "");
            return;
        }

        final double pvpPrice = plugin.getConfig().getDouble("Terrenos.PvP price");

        if (protectedRegion.getFlag(DefaultFlag.PVP) == StateFlag.State.DENY) {
            inv.setItem(34, new MakeItem(Material.WOOD_SWORD)
                    .setName("<b>PvP")
                    .addLoreList(
                            "",
                            " <7>PvP: <c>Desativado<7>. ",
                            " <7>Clique aqui para ativar. <8>(10k) ",
                            "")
                    .build(), event -> {

                player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

                Confirmation.confirm("Confirmação:", new MakeItem(Material.WOOD_SWORD)
                        .setName("<b>PvP")
                        .addLoreList(
                                "",
                                " <7>PvP: <c>Desativado<7>. ",
                                " <7>Você irá ativar. ",
                                "")
                        .build(), player, accept -> {

                    if (!economy.has(player, pvpPrice)) {
                        double needed = pvpPrice - economy.getBalance(player);

                        sendMessage(player, Sound.VILLAGER_NO,
                                " <6>☣ <c>Você precisa de mais <f>R$ " + NumberUtil.formatNumberSimple(needed, '.') + " <c>para ativar o PvP.");
                        player.closeInventory();
                        return;
                    }

                    protectedRegion.setFlag(DefaultFlag.PVP, StateFlag.State.ALLOW);

                    economy.withdrawPlayer(player, pvpPrice);

                    sendMessage(player, Sound.SUCCESSFUL_HIT,
                            " <2>✔ <a>Você ativou o PvP do terreno <f>" + terrain + "<a>!");
                    player.closeInventory();

                    try {
                        regionManager.save();
                    } catch (StorageException exception) {
                        sendMessage(player, Sound.VILLAGER_NO,
                                " <6>☣ <c>Ocorreu um erro ao tenta salvar o terreno.");
                    }

                }, reject -> {

                    areaMenu(player, terrain);
                    player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

                });
            });

        } else {

            inv.setItem(34, new MakeItem(Material.DIAMOND_SWORD)
                    .setName("<b>PvP")
                    .addLoreList("", " <7>PvP: <a>Ativado<7>. ", " <7>Clique aqui para desativar. <8>(10k) ", "")
                    .build(), event -> {

                player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

                Confirmation.confirm("Confirmação:", new MakeItem(Material.DIAMOND_SWORD)
                        .setName("<b>PvP")
                        .addLoreList(
                                "",
                                " <7>PvP: <a>Ativado<7>. ",
                                " <7>Você irá desativar. ",
                                "")
                        .build(), player, accept -> {

                    if (!economy.has(player, plugin.getConfig().getDouble("Settings.PvP price"))) {
                        double needed = plugin.getConfig().getDouble("Settings.PvP price") - economy.getBalance(player);

                        sendMessage(player, Sound.VILLAGER_NO,
                                " <6>☣ <c>Você precisa de mais <f>R$ " + NumberUtil.formatNumberSimple(needed, '.') + " <c>para desativar o PvP. ");

                        player.closeInventory();
                        return;
                    }

                    economy.withdrawPlayer(player, plugin.getConfig().getDouble("Terrenos.PvP price"));

                    protectedRegion.setFlag(DefaultFlag.PVP, StateFlag.State.DENY);

                    sendMessage(player, Sound.SUCCESSFUL_HIT,
                            " <2>✔ <a>Você desativou o PvP do terreno <f>" + terrain + "<a>!");
                    player.closeInventory();

                    try {
                        regionManager.save();
                    } catch (StorageException exception) {
                        sendMessage(player, Sound.VILLAGER_NO,
                                " <6>☣ <c>Ocorreu um erro ao salvar o terreno.");
                    }

                }, reject -> {

                    areaMenu(player, terrain);
                    player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

                });
            });
        }

        final double entryPrice = plugin.getConfig().getDouble("Settings.Entry price");

        if (protectedRegion.getFlag(DefaultFlag.ENTRY) == null) {

            inv.setItem(33, new MakeItem(Material.BARRIER)
                    .setName("<b>Entrada")
                    .addLoreList(
                            "",
                            " <7>Status: <a>Ativado<7>.",
                            " <7>Clique aqui para desativar. <8>(10k) ",
                            "")
                    .build(), event -> {

                player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

                Confirmation.confirm("Confirmação:", new MakeItem(Material.BARRIER)
                        .setName("<b>Entrada")
                        .addLoreList(
                                "",
                                " <7>Status: <a>Ativado<7>. ",
                                " <7>Você irá desativar. ",
                                "")
                        .build(), player, accept -> {

                    if (!economy.has(player, entryPrice)) {
                        double needed = entryPrice - economy.getBalance(player);

                        sendMessage(player, Sound.VILLAGER_NO,
                                " <6>☣ <c>Você precisa de mais <f>R$ " + NumberUtil.formatNumberSimple(needed, '.') + " <c>para desativar a entrada.");
                        return;
                    }

                    economy.withdrawPlayer(player, entryPrice);
                    protectedRegion.setFlag(DefaultFlag.ENTRY, StateFlag.State.DENY);

                    sendMessage(player, Sound.SUCCESSFUL_HIT,
                            " <2>✔ <a>Você desativou a entrada no terreno <f>" + terrain + "<a>!");
                    player.closeInventory();

                    try {
                        regionManager.save();
                    } catch (StorageException exception) {
                        sendMessage(player, Sound.VILLAGER_NO,
                                " <6>☣ <c>Ocorreu um erro ao tenta salvar o terreno.");
                    }

                }, reject -> {

                    areaMenu(player, terrain);
                    player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

                });
            });

        } else {

            inv.setItem(33, new MakeItem(Material.BARRIER)
                    .setName("<b>Entrada")
                    .addLoreList("", " <7>Status: <c>Desativado<7>. ", " <7>Clique aqui para ativar. <8>(10k) ", "")
                    .build(), event -> {

                player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

                Confirmation.confirm("Confirmação:", new MakeItem(Material.BARRIER)
                        .setName("<b>Entrada")
                        .addLoreList(
                                "",
                                " <7>Status: <c>Desativado<7>. ",
                                " <7>Você irá ativar. ",
                                "")
                        .build(), player, accept -> {

                    if (!economy.has(player, entryPrice)) {
                        double needed = entryPrice - economy.getBalance(player);

                        sendMessage(player, Sound.VILLAGER_NO,
                                " <6>☣ <c>Você precisa de mais <f>R$ " + NumberUtil.formatNumberSimple(needed, '.') + " <c>para ativar a entrada.");
                        return;
                    }

                    economy.withdrawPlayer(player, entryPrice);
                    protectedRegion.setFlag(DefaultFlag.ENTRY, null);

                    sendMessage(player, Sound.SUCCESSFUL_HIT,
                            " <2>✔ <a>Você ativou a entrada no terreno <f>" + terrain + "<a>!");
                    player.closeInventory();

                    try {
                        regionManager.save();
                    } catch (StorageException exception) {
                        sendMessage(player, Sound.VILLAGER_NO,
                                " <6>☣ <c>Ocorreu um erro ao salvar o terreno.");
                    }

                }, reject -> {

                    areaMenu(player, terrain);
                    player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

                });
            });

        }

        inv.setItem(13, new MakeItem(player.getName())
                .of("b465f80bf02b408885987b00957ca5e9eb874c3fa88305099597a333a336ee15")
                .setName("<b>Deletar")
                .addLoreList("", " <7>Clique aqui para deletar o terreno <f>" + terrain + "<7>. ", "")
                .build(), event -> {

            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

            Confirmation.confirm("Confirmação:",
                    new MakeItem(player.getName())
                            .of("b465f80bf02b408885987b00957ca5e9eb874c3fa88305099597a333a336ee15")
                            .setName("<e>Excluir terreno")
                            .addLoreList("",
                                    " <7>Deseja deletar o terreno <f>" + terrain + "<7>?",
                                    "")
                            .build(),
                    player,
                    accept -> {

                        terrainManager.delTerrain(player, terrain);

                        player.closeInventory();
                        player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);

                    }, reject -> {

                        areaMenu(player, terrain);
                        player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

                    });
        });

        String lore;
        if (protectedRegion.getMembers().size() != 0) lore = " <7>Clique aqui para ver os membros. ";
        else lore = " <7>Você não possui nenhum membro. ";

        inv.setItem(28, new MakeItem(player.getName())
                .setName("<b>Membros")
                .addLoreList("", lore, "")
                .build(), event -> {

            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
            friendMenu(player, terrain);

        });

        inv.setItem(29, new MakeItem(Material.ENDER_PEARL)
                .setName("<b>Teleportar")
                .addLoreList(
                        "",
                        " <7>Clique aqui para ir ao seu terreno. ",
                        " <7>Utilize caso tenha perdido seu terreno! ",
                        "")
                .build(), event -> {

            if (cooldowns.contains(player.getName())) {
                sendMessage(player, Sound.VILLAGER_NO,
                        " <6>☣ <c>Você precisa esperar para se teleportar novamente.");
                return;
            }

            final double upX = protectedRegion.getMaximumPoint().getBlockX() + 1;
            final double upZ = protectedRegion.getMaximumPoint().getBlockZ() + 1;

            final double lowerX = protectedRegion.getMinimumPoint().getBlockX();
            final double lowerZ = protectedRegion.getMinimumPoint().getBlockZ();

            final double x = lowerX + (upX - lowerX) / 2;
            final double z = lowerZ + (upZ - lowerZ) / 2;

            final World world = plugin.getServer().getWorld(plugin.getConfig().getString("Settings.World"));
            final Location location = new Location(world, x, 4, z);

            location.setYaw(player.getLocation().getYaw());
            location.setPitch(player.getLocation().getPitch());

            if (!LocationUtil.isLocationSafe(location)) {
                sendMessage(player, Sound.VILLAGER_NO,
                        " <6>☣ <c>O teleporte até sua casa não é seguro!");
                return;
            }

            cooldowns.add(player.getName());
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> cooldowns.remove(player.getName()), plugin.getConfig().getInt("Settings.Teleport delay") * 20L);

            player.closeInventory();
            player.teleport(location);
            player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 1);

        });

        inv.setItem(49, new MakeItem(Material.ARROW)
                .setName("<b>Voltar")
                .addLoreList(
                        "",
                        " <7>Clique aqui para voltar. ",
                        "")
                .build(), event -> {

            openAreas(player, false);
            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

        });

        final Terrain terrainOBJ = terrainAPI.getTerrain(terrain, player.getName());

        if (terrainOBJ != null) {
            inv.setItem(31, new MakeItem("wi_b")
                    .of("bf1ac7182f91efcc274d6dd387e75d21227f1ad0a06b07253d43ccc9dea29ff")
                    .setName("<b>IPTU")
                    .addLoreList(
                            "",
                            " <7>Gerenciamento do IPTU. ",
                            "").build(), event -> {

                player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
                iptuMenu(player, terrainOBJ);

            });
        }

        inv.open(player);

    }

    private final String green = "22d145c93e5eac48a661c6f27fdaff5922cf433dd627bf23eec378b9956197";
    private final String red = "5fde3bfce2d8cb724de8556e5ec21b7f15f584684ab785214add164be7624b";

    public void iptuMenu(Player player, Terrain terrain) {
        final Inv inv = new Inv(27, "IPTU do terreno " + terrain.getName() + ":");
        inv.setIgnorePlayerInventoryClick(true, true);

        final String overdue = (System.currentTimeMillis() > terrain.getExpiration() ? "<c>Vencido" : "<a>Pago") + "<7>.";

        inv.setItem(10, new MakeItem("CrynexTm")
                .of(terrain.isAutomatic() ? green : red)
                .setName("<b>IPTU automático")
                .addLoreList(
                        "",
                        " <7>Status: " + (terrain.isAutomatic() ? "<a>Ativado" : "<c>Desativado") + "<7>. ",
                        " <7>Clique aqui para " + (terrain.isAutomatic() ? "<c>desativar" : "<a>ativar") + "<7>. ",
                        "")
                .build(), event -> {

            terrain.setAutomatic(!terrain.isAutomatic());
            terrain.saveAsync();

            inv.updateItem(10, new MakeItem("CrynexTm")
                    .of(terrain.isAutomatic() ? green : red)
                    .setName("<b>IPTU automático")
                    .addLoreList("",
                            " <7>Status: " + (terrain.isAutomatic() ? "<a>Ativado" : "<c>Desativado") + "<7>. ",
                            " <7>Clique aqui para " + (terrain.isAutomatic() ? "<c>desativar" : "<a>ativar") + "<7>. ",
                            "")
                    .build());

            inv.updateItem(16, new MakeItem("SteveNugget")
                    .of(terrain.isAutomatic() ? red : green)
                    .setName("<b>IPTU manual")
                    .addLoreList("",
                            " <7>Status: " + (terrain.isAutomatic() ? "<c>Desativado" : "<a>Ativado") + "<7>. ",
                            " <7>Clique aqui para " + (terrain.isAutomatic() ? "<a>ativar" : "<c>desativar") + "<7>. ",
                            "")
                    .build());

            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
            sendMessage(player, " <2>✔ <a>Você " + (terrain.isAutomatic() ? "ativou" : "desativou") + " o IPTU automático!");

        });

        inv.setItem(13, new MakeItem("wi_b")
                .of("bf1ac7182f91efcc274d6dd387e75d21227f1ad0a06b07253d43ccc9dea29ff")
                .setName("<b>IPTU")
                .addLoreList("",
                        " <7>Status: " + overdue,
                        " <7>Próximo IPTU: <f>" + TimeFormat.getTime(terrain.getExpiration()) + " ",
                        " <7>Valor: <f>R$ " + NumberUtil.formatNumberSimple(terrain.getIPTUPrice(), '.') + " ",
                        "")
                .build(), event -> {

            if (!terrain.isExpired()) {
                sendMessage(player, Sound.VILLAGER_NO,
                        " <6>☣ <c>Você precisa esperar até o dia <f>" + TimeFormat.getTime(terrain.getExpiration()) + " <c>para pagar o <f>IPTU<c>!");
                return;
            }

            if (economy.has(player, terrain.getIPTUPrice())) {

                economy.withdrawPlayer(player, terrain.getIPTUPrice());

                terrain.updateExpiration();
                terrain.setBlocked(false);
                terrain.saveAsync();

                inv.updateItem(13, new MakeItem("_Racks_")
                        .of("7cc4cf8a56a01fa4794184aa11d1b603b76df16a8282dfc10e9c46060d32768a")
                        .setName("<b>Pagar IPTU")
                        .addLoreList("",
                                " <7>Status: " + (System.currentTimeMillis() > terrain.getExpiration() ? "<c>Vencido" : "<a>Pago") + "<7>. ",
                                " <7>Próximo IPTU: <f>" + TimeFormat.getTime(terrain.getExpiration()) + " ",
                                " <7>Valor: <f>R$ " + NumberUtil.formatNumberSimple(terrain.getIPTUPrice(), '.') + " ",
                                "").build());

                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1, 1);

                sendMessage(player, Sound.SUCCESSFUL_HIT,
                        "",
                        " <2>✔ <a>Você pagou o <f>IPTU <a>com sucesso!",
                        " <2>✔ <a>Próxima expiração: <f>" + TimeFormat.getTime(terrain.getExpiration()),
                        "");

            } else {
                double needed = terrain.getIPTUPrice() - economy.getBalance(player);

                sendMessage(player, Sound.VILLAGER_NO,
                        " <6>☣ <c>Você precisa de mais <f>R$ " + NumberUtil.formatNumberSimple(needed, '.') + " <c>para pagar o IPTU.");
            }
        });

        inv.setItem(16, new MakeItem("SteveNugget")
                .of(terrain.isAutomatic() ? red : green)
                .setName("<b>IPTU manual")
                .addLoreList("",
                        " <7>Status: " + (terrain.isAutomatic() ? "<c>Desativado" : "<a>Ativado") + "<7>. ",
                        " <7>Clique aqui para " + (terrain.isAutomatic() ? "<a>ativar" : "<c>desativar") + "<7>. ",
                        "")
                .build(), event -> {

            terrain.setAutomatic(!terrain.isAutomatic());
            terrain.saveAsync();

            inv.updateItem(16, new MakeItem("SteveNugget")
                    .of(terrain.isAutomatic() ? red : green)
                    .setName("<b>IPTU manual")
                    .addLoreList("",
                            " <7>Status: " + (terrain.isAutomatic() ? "<c>Desativado" : "<a>Ativado") + "<7>. ",
                            " <7>Clique aqui para " + (terrain.isAutomatic() ? "<a>ativar" : "<c>desativar") + "<7>. ",
                            "")
                    .build());

            inv.updateItem(10, new MakeItem("CrynexTm")
                    .of(terrain.isAutomatic() ? green : red)
                    .setName("<b>IPTU automático")
                    .addLoreList("",
                            " <7>Status: " + (terrain.isAutomatic() ? "<a>Ativado" : "<c>Desativado") + "<7>. ",
                            " <7>Clique aqui para " + (terrain.isAutomatic() ? "<c>desativar" : "<a>ativar") + "<7>. ",
                            "")
                    .build());

            sendMessage(player, Sound.CLICK,
                    " <2>✔ <a>Você " + (terrain.isAutomatic() ? "desativou" : "ativou") + " o IPTU manual!");

        });

        inv.setItem(26, new MakeItem(Material.ARROW).setName("<b>Voltar")
                .addLoreList(
                        "",
                        " <7>Clique aqui para voltar.",
                        "")
                .build(), event -> {

            areaMenu(player, terrain.getName());
            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

        });

        inv.open(player);
        player.updateInventory();
    }

    public void friendMenu(Player player, String area) {
        final Inv inv = new Inv(54, "Membros do terreno " + area + ":");
        inv.setIgnorePlayerInventoryClick(true, true);

        final RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());
        final ProtectedRegion protectedRegion = regionManager.getRegion(player.getName().toLowerCase() + "-" + area);

        if (protectedRegion == null) {
            sendMessage(player, " <6>☣ <c>O terreno <f>" + area + " <c>não foi encontrado.");
            return;
        }

        boolean hasFriends = true;
        if (protectedRegion.getMembers().size() == 0) {

            inv.setItem(22, new MakeItem(Material.PAPER)
                    .setName(" <c>Você não tem nenhum amigo.")
                    .build());

            hasFriends = false;
        }

        String status;

        if (hasFriends) {
            for (String member : protectedRegion.getMembers().getPlayers()) {

                if (plugin.getServer().getPlayer(member) == null) status = "<c>Offline";
                else status = "<a>Online";


                String correctlyName = member;

                if (plugin.getServer().getOfflinePlayer(member).hasPlayedBefore())
                    correctlyName = plugin.getServer().getOfflinePlayer(member).getName();

                final String finalStatus = status;
                final String finalCorrectlyName = correctlyName;

                inv.setInMiddle(new MakeItem(member)
                        .setName("<e>" + correctlyName)
                        .addLoreList(
                                "",
                                " <7>Clique aqui para remove-lo do terreno. ",
                                " <7>Status: " + status,
                                "")
                        .build(), event -> {

                    player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

                    Confirmation.confirm("Confirmação:", new MakeItem(member)
                                    .setName("<e>" + finalCorrectlyName)
                                    .addLoreList(
                                            "",
                                            " <7>Clique aqui para remove-lo do terreno. ",
                                            " <7>Status: " + finalStatus,
                                            "")
                                    .build(),
                            player,
                            accept -> terrainManager.delFriend(player, member, player.getName().toLowerCase() + "-" + area),
                            reject -> areaMenu(player, area));
                });
            }
        }

        inv.setItem(49, new MakeItem(Material.ARROW)
                .setName(" <e>Voltar")
                .addLoreList(
                        "",
                        " <7>Clique aqui para voltar. ",
                        "")
                .build(), event -> {

            areaMenu(player, area);
            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);

        });

        inv.setItem(53, new MakeItem(Material.SIGN)
                .setName(" <b>Adicionar alguém")
                .addLoreList(
                        "",
                        " <7>Clique aqui para adicionar alguém. ",
                        "")
                .setGlow(true)
                .build(), event -> {

            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
            player.closeInventory();

            sendMessage(player,
                    "",
                    " <a>Digite o nome de alguém para adiciona-lo!",
                    " <a>Para cancelar digite \"<f>cancelar<a>\".",
                    " <c>Máximo de <f>16 caracteres <c>e sem <f>espaços<c>!",
                    "");

            GetValueFromPlayerChat.getValueFrom(player, "cancelar", true, onGet -> {
                String userName = onGet.getValueString();

                if (userName.length() > 16 || userName.contains(" ")) {
                    sendMessage(player, Sound.VILLAGER_NO,
                            " <6>☣ <c>O nome só pode ter <f>16 caracteres <c>e não pode conter <f>espaços<c>.");
                    onGet.repeatGetValueFrom();
                    return;
                }

                final Player target = plugin.getServer().getPlayer(userName.trim());

                if (target == null) {
                    sendMessage(player, Sound.VILLAGER_NO,
                            userName + " <c>não está online.");
                    onGet.repeatGetValueFrom();
                    return;
                }

                if (player.getName().equalsIgnoreCase(target.getName())) {
                    sendMessage(player, Sound.VILLAGER_NO,
                            " <6>☣ <c>Você não pode adicionar você mesmo ao terreno.");
                    onGet.repeatGetValueFrom();
                    return;
                }

                terrainManager.addFriend(player, target, area);
                friendMenu(player, area);

            }, onCancel -> {

                friendMenu(player, area);
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1, 1);

            });
        });

        inv.open(player);
    }

    void sendMessage(Player player, String... texts) {
        TXT.sendMessages(player, texts);
    }

    void sendMessage(Player player, Sound sound, String... texts) {
        TXT.sendMessages(player, texts);

        player.playSound(player.getLocation(), sound, 1, 1);
    }
}
