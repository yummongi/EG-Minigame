package kr.egsuv.minigames;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MapSelectionGUI implements Listener {
    private final EGServerMain plugin;
    private final Minigame game;
    private final Inventory gui;
    private final Map<String, ItemStack> mapIcons;
    private String selectedMap;
    private boolean isSelecting = false;
    private int currentMapIndex = 0;
    private List<String> mapList;

    public MapSelectionGUI(EGServerMain plugin, Minigame game, Map<String, ItemStack> mapIcons) {
        this.plugin = plugin;
        this.game = game;
        this.mapIcons = mapIcons;
        this.gui = Bukkit.createInventory(null, 27, Component.text("맵 선택 중...").color(NamedTextColor.BLACK).decorate(TextDecoration.BOLD));
        this.mapList = new ArrayList<>(game.getConfig().getMaps());
        Collections.shuffle(mapList);
        initializeGUI();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void initializeGUI() {
        ItemStack borderItem = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i < 10 || i > 16 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, borderItem);
            }
        }

        if (!mapList.isEmpty()) {
            updateGUIWithMap(mapList.get(0));
        }

        ItemStack infoItem = createItem(Material.BOOK, "§e§l맵 정보", "§7클릭하여 현재 맵의 정보를 확인하세요.");
        gui.setItem(22, infoItem);
    }

    public void show() {
        if (mapList == null || mapList.isEmpty()) {
            plugin.getLogger().warning("맵 목록이 비어있습니다. 게임을 시작할 수 없습니다.");
            for (Player player : game.getPlayers()) {
                player.sendMessage(Prefix.SERVER + "맵을 불러올 수 없어 게임을 시작할 수 없습니다.");
            }
            game.endGame(true);
            game.teleportToSpawnLobbyAllPlayers();
            return;
        }

        isSelecting = true;
        for (Player player : game.getPlayers()) {
            player.openInventory(gui);
            player.setGameMode(GameMode.SPECTATOR);
        }

        new BukkitRunnable() {
            int ticks = 0;
            int speed = 1;

            @Override
            public void run() {
                if (ticks >= 100) {
                    cancel();
                    finalizeMapSelection();
                    plugin.getLogger().info("맵 목록: " + mapList);
                    plugin.getLogger().info("맵 아이콘: " + mapIcons.keySet());
                    return;
                }

                if (ticks % speed == 0) {
                    currentMapIndex = (currentMapIndex + 1) % mapList.size();
                    String nextMap = mapList.get(currentMapIndex);
                    updateGUIWithMap(nextMap);
                    playMapChangeSound();
                }

                if (ticks % 20 == 0) {
                    speed++;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finalizeMapSelection() {
        selectedMap = mapList.get(currentMapIndex);
        isSelecting = false;

        for (Player player : game.getPlayers()) {
            player.closeInventory();
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(Prefix.SERVER + "선택된 맵: §e" + selectedMap);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.setWalkSpeed(0.2f); // 기본 이동 속도로 복원
        }

        game.setCurrentMap(selectedMap);
        Bukkit.getScheduler().runTaskLater(plugin, () -> game.startGameAfterMapSelection(), 20L); // 1초 후 게임 시작
    }


    private void updateGUIWithMap(String mapName) {
        if (mapName == null || !mapIcons.containsKey(mapName)) {
            game.endGame(true);
            game.teleportToSpawnLobbyAllPlayers();
            game.broadcastToPlayers(Component.text(Prefix.SERVER + "현재 게임에 오류가 발생했습니다. 관리자에게 문의해주세요."));
        }
        ItemStack icon = mapIcons.getOrDefault(mapName, new ItemStack(Material.PAPER));
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(icon.getType());
        }

        meta.displayName(Component.text(mapName).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("현재 선택된 맵").color(NamedTextColor.GRAY));
        meta.lore(lore);

        icon.setItemMeta(meta);
        gui.setItem(13, icon);

        for (Player player : game.getPlayers()) {
            player.updateInventory();
        }
    }
    private void playMapChangeSound() {
        for (Player player : game.getPlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));

        if (lore.length > 0) {
            List<Component> loreComponents = new ArrayList<>();
            for (String loreLine : lore) {
                loreComponents.add(Component.text(loreLine));
            }
            meta.lore(loreComponents);
        }

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().equals(gui)) {
            event.setCancelled(true);
            if (event.getSlot() == 22 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BOOK) {
                showMapInfo((Player) event.getWhoClicked());
            }
        }
    }

    private void showMapInfo(Player player) {
        String currentMap = mapList.get(currentMapIndex);
        player.sendMessage(Prefix.SERVER + "§e=== " + currentMap + " 맵 정보 ===");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (isSelecting && event.getInventory().equals(gui)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (isSelecting) {
                    event.getPlayer().openInventory(gui);
                }
            });
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isSelecting && game.getPlayers().contains(event.getPlayer())) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                    event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                    event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setCancelled(true);
            }
        }
    }

    public String getSelectedMap() {
        return selectedMap;
    }
}