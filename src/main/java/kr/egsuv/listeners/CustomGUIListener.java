package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.data.MinigameData;
import kr.egsuv.data.PlayerData;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CustomGUIListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private final String MAIN_GUI_TITLE = "§0미니게임 메뉴";
    private final String TARGET_ITEM_NAME = "게임 메뉴";
    private Map<Player, Inventory> playerGUIs = new HashMap<>();

    public CustomGUIListener() {
        startGUIUpdater();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.NETHER_STAR && hasCustomName(item, TARGET_ITEM_NAME)) {
            openGUI(event.getPlayer());
            event.setCancelled(true);
        }
    }

    private void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, getUpdatedGUITitle());

        int firstHitGameCurrentPlayers = plugin.getFirstHitGame().getPlayers().size() < 1 ? 1 : plugin.getFirstHitGame().getPlayers().size();
        // 사용자 정의 이름과 설명이 있는 아이콘 생성
        ItemStack firstHitIcon = ItemUtils.createItem(Material.DIAMOND, firstHitGameCurrentPlayers, "선빵 게임 참가", "선빵 게임 게임에 참가하려면 클릭하세요!");
        ItemStack rankingIcon = ItemUtils.createItem(Material.GOLD_HELMET, 1, "랭킹 보기", "클릭하여 게임 랭킹을 확인하세요!");
        ItemStack teamDeathmatchIcon = ItemUtils.createItem(Material.IRON_SWORD, 1, "팀 데스매치 참가", "팀 데스매치 게임에 참가하려면 클릭하세요!");

        // 아이콘을 GUI에 추가
        gui.setItem(11, rankingIcon);
        gui.setItem(14, firstHitIcon);
        gui.setItem(15, teamDeathmatchIcon);

        player.openInventory(gui);
        playerGUIs.put(player, gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(MAIN_GUI_TITLE)) {
            event.setCancelled(true); // GUI 내 아이템 이동 방지
            if (event.getClickedInventory() != event.getView().getBottomInventory()) {
                Player player = (Player) event.getWhoClicked();
                ItemStack currentItem = event.getCurrentItem();
                if (currentItem != null) {
                    switch (currentItem.getType()) {
                        case DIAMOND:
                            player.performCommand("firsthit join");
                            player.sendMessage("첫 타격 미니게임에 참가했습니다!");
                            player.closeInventory();
                            break;
                        case GOLD_HELMET:
                            showRankings(player);
                            break;

                        case IRON_SWORD:
                            player.performCommand("teamdeathmatch join");
                            player.sendMessage("팀 데스매치 게임에 참가했습니다!");
                            player.closeInventory();
                            break;
                    }
                }
            } else {
                // 플레이어 인벤토리에서 아이템을 클릭한 경우, 툴바로 옮기려 하면 아이템 제거
                if (event.getSlot() < 9 && event.getCurrentItem() != null) {
                    event.getCurrentItem().setAmount(0);
                }
            }
        }
    }


    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.NETHER_STAR && hasCustomName(item, TARGET_ITEM_NAME)) {
            event.setCancelled(true);
            event.getItemDrop().remove();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith(MAIN_GUI_TITLE)) {
            playerGUIs.remove(event.getPlayer());
        }
    }




    private boolean hasCustomName(ItemStack item, String name) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName().contains(name);
        }
        return false;
    }

    private String getUpdatedGUITitle() {
        int onlinePlayersCount = plugin.getServer().getOnlinePlayers().size();
        return MAIN_GUI_TITLE + " (현재 로비 인원 수: " + onlinePlayersCount + ")";
    }

    private void startGUIUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : playerGUIs.keySet()) {
                    if (player.getOpenInventory().getTitle().startsWith(MAIN_GUI_TITLE)) {
                        openGUI(player); // GUI를 닫고 다시 엽니다.
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 매 1초(20 틱)마다 업데이트
    }

    private void showRankings(Player player) {
        Inventory rankingGUI = Bukkit.createInventory(null, 54, "§0게임 랭킹");

        for (Minigame minigame : plugin.getMinigameList()) {
            List<Map.Entry<String, Double>> topPlayers = plugin.getRankingManager().getTopPlayers(minigame.getCOMMAND_MAIN_NAME(), 10);

            ItemStack icon = new ItemStack(Material.GOLD_BLOCK);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName("§6" + minigame.getDisplayGameName() + " 랭킹");
            List<String> lore = new ArrayList<>();

            for (int i = 0; i < topPlayers.size(); i++) {
                Map.Entry<String, Double> entry = topPlayers.get(i);
                lore.add(String.format("§e%d. §f%s - §b%.2f점", i + 1, entry.getKey(), entry.getValue()));
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
            rankingGUI.addItem(icon);
        }

        player.openInventory(rankingGUI);
    }

    private void showPlayerStats(Player player) {
        Inventory statsGUI = Bukkit.createInventory(null, 54, "§0게임 통계");

        for (Minigame minigame : plugin.getMinigameList()) {
            PlayerData playerData = plugin.getDataManager().getPlayerData(player);
            MinigameData data = playerData.getMinigameData(minigame.getCOMMAND_MAIN_NAME(), minigame.isTeamGame());

            ItemStack icon = new ItemStack(Material.BOOK);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName("§6" + minigame.getDisplayGameName());
            List<String> lore = new ArrayList<>();

            if (data.isTeamGame()) {
                int wins = data.getWins();
                int losses = data.getLosses();
                double winRate = data.getWinRate() * 100;
                lore.add("§e승리: §a" + wins);
                lore.add("§e패배: §c" + losses);
                lore.add("§e승률: §b" + String.format("%.2f", winRate) + "%");
            } else {
                for (int i = 1; i <= 10; i++) {
                    lore.add("§e" + i + "등: §a" + data.getRankCount(i) + "회");
                }
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
            statsGUI.addItem(icon);
        }

        player.openInventory(statsGUI);
    }
}