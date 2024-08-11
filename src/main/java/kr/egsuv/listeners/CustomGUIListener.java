package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.data.MinigameData;
import kr.egsuv.data.PlayerData;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CustomGUIListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private final String MAIN_GUI_TITLE = "§0미니게임 메뉴";
    private final String TARGET_ITEM_NAME = "게임 메뉴";
    private Map<Player, Inventory> playerGUIs = new HashMap<>();

    private final Map<Player, BukkitTask> updateTasks = new HashMap<>();

    //20 * 3 = 3초마다 GUI 업데이트
    private final long UPDATE_TASK_TIME = 60L;

    public CustomGUIListener() {
        startGUIUpdater();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        if (item.getType() == Material.NETHER_STAR && hasCustomName(item, TARGET_ITEM_NAME)) {
            openGUI(player);
            event.setCancelled(true);
        } else if (item.getType() == Material.ENCHANTED_BOOK && hasCustomName(item, "§7[ §f게임 §a튜토리얼 §f읽기 §7]")) {
            Minigame currentGame = getCurrentMinigame(player);
            if (currentGame != null) {
                currentGame.showRules(player);
            }
            event.setCancelled(true);
        } else if (item.getType() == Material.DARK_OAK_DOOR && hasCustomName(item, "§7[ §f게임 §c나가기 §7]")) {
            plugin.teleportToSpawn(player);
            event.setCancelled(true);
        } else if (item.getType() == Material.PLAYER_HEAD && hasCustomName(item, "§7[ §f현재 §a참가중인 유저 §f보기 §7]")) {
            event.setCancelled(true);
            openPlayerStatsGUI(event.getPlayer());
        }
    }

    private void openPlayerStatsGUI(Player player) {
        Minigame currentGame = plugin.getCurrentGame(player);
        if (currentGame == null) {
            player.sendMessage(Prefix.SERVER + "현재 게임 또는 게임 로비에 있지 않습니다.");
            return;
        }

        updatePlayerStatsGUI(player, currentGame);

        // 실시간 업데이트 태스크 시작
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getOpenInventory().getTitle().equals("참가 중인 플레이어")) {
                    updatePlayerStatsGUI(player, currentGame);
                } else {
                    this.cancel();
                    updateTasks.remove(player);
                }
            }
        }.runTaskTimer(plugin, 20L, UPDATE_TASK_TIME); // 3초마다 업데이트

        updateTasks.put(player, task);
    }

    private void updatePlayerStatsGUI(Player player, Minigame currentGame) {
        Set<Player> gamePlayers = new HashSet<>(currentGame.getPlayers());
        for (Player p : currentGame.getPlayers()) {
            if (currentGame.isPlayerInGameLobby(p)) {
                gamePlayers.add(p);
            }
        }

        int size = Math.min(54, ((gamePlayers.size() - 1) / 9 + 1) * 9); // 최대 54칸
        Inventory gui = player.getOpenInventory().getTopInventory();
        if (gui.getSize() != size) {
            // 크기가 변경되었다면 새 인벤토리 생성
            gui = Bukkit.createInventory(null, size, Component.text("참가 중인 플레이어"));
            player.openInventory(gui);
        } else {
            gui.clear(); // 기존 아이템 제거
        }

        for (Player gamePlayer : gamePlayers) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(gamePlayer);
            meta.displayName(Component.text(gamePlayer.getName()).color(NamedTextColor.YELLOW));

            PlayerData playerData = plugin.getDataManager().getPlayerData(gamePlayer);
            MinigameData gameData = playerData.getMinigameData(currentGame.getCOMMAND_MAIN_NAME(), currentGame.isTeamGame());
            double score = plugin.getRankingManager().getPlayerScore(currentGame.getCOMMAND_MAIN_NAME(), gamePlayer.getName());

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("점수: " + String.format("%.2f", score)).color(NamedTextColor.GOLD));
            lore.add(Component.text("총 게임 수: " + gameData.getTotalGames()).color(NamedTextColor.AQUA));

            if (currentGame.isTeamGame()) {
                lore.add(Component.text("승리: " + gameData.getWins()).color(NamedTextColor.GREEN));
                lore.add(Component.text("패배: " + gameData.getLosses()).color(NamedTextColor.RED));
                lore.add(Component.text("무승부: " + gameData.getDraws()).color(NamedTextColor.GRAY));
                double winRate = gameData.getTotalGames() > 0 ? (double) gameData.getWins() / gameData.getTotalGames() * 100 : 0;
                lore.add(Component.text("승률: " + String.format("%.2f%%", winRate)).color(NamedTextColor.YELLOW));
            } else {
                for (int i = 1; i <= 3; i++) {
                    lore.add(Component.text(i + "등: " + gameData.getRankCount(i) + "회").color(NamedTextColor.GRAY));
                }
            }

            meta.lore(lore);
            head.setItemMeta(meta);
            gui.addItem(head);
        }
    }


    private Minigame getCurrentMinigame(Player player) {
        for (Minigame minigame : plugin.getMinigameList()) {
            if (minigame.getPlayers().contains(player)) {
                return minigame;
            }
        }
        return null;
    }



    private void openGUI(Player player) {
        int size = (plugin.getMinigameList().size() / 9 + 1) * 9;
        Inventory gui = Bukkit.createInventory(null, size, Component.text(getUpdatedGUITitle()));

        for (int i = 0; i < plugin.getMinigameList().size(); i++) {
            Minigame minigame = plugin.getMinigameList().get(i);
            ItemStack icon = createMinigameIcon(minigame);
            gui.setItem(i, icon);
        }

        player.openInventory(gui);
        playerGUIs.put(player, gui);
    }

    private ItemStack createMinigameIcon(Minigame minigame) {
        ItemStack icon = new ItemStack(Material.PAPER);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text(minigame.getDisplayGameName()).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        int currentPlayers = minigame.getPlayers().size();
        lore.add(Component.text(String.format("플레이어: %d/%d [%s]", currentPlayers, minigame.getMAX_PLAYER(), getStateString(minigame.getState()))).color(NamedTextColor.GRAY));

        String teamTypeString = minigame.isRedBlueTeamGame ? "레드팀 vs 블루팀" : minigame.getTeamType().name();
        lore.add(Component.text("게임 모드: " + teamTypeString).color(NamedTextColor.AQUA));

        if (minigame.getState() == MinigameState.IN_PROGRESS) {
            lore.add(Component.text("남은 시간: " + minigame.getGameTimeLimit() + "초").color(NamedTextColor.YELLOW));
        }

        lore.add(Component.text("클릭하여 입장").color(NamedTextColor.GREEN));

        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private String getStateString(MinigameState state) {
        switch (state) {
            case WAITING: return "§a대기중";
            case STARTING: return "§a시작중";
            case IN_PROGRESS: return "§c진행중";
            case ENDING: return "§c종료 중";
            case DISABLED:
                return "§c점검중";
            default: return "알 수 없음";
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (title.startsWith(MAIN_GUI_TITLE)) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getBottomInventory()) {
                Player player = (Player) event.getWhoClicked();
                ItemStack currentItem = event.getCurrentItem();
                if (currentItem != null && currentItem.getType() == Material.PAPER) {
                    String gameName = PlainTextComponentSerializer.plainText().serialize(currentItem.getItemMeta().displayName());
                    Minigame selectedGame = plugin.getMinigameList().stream()
                            .filter(game -> game.getDisplayGameName().equals(gameName))
                            .findFirst()
                            .orElse(null);

                    if (selectedGame != null) {
                        selectedGame.joinGame(player);
                    }
                    player.closeInventory();
                }
            }
        } else if (event.getView().title().equals(Component.text("참가 중인 플레이어"))) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    Player clickedPlayer = meta.getOwningPlayer().getPlayer();
                    if (clickedPlayer != null) {
                        Player viewer = (Player) event.getWhoClicked();
                        Minigame currentGame = plugin.getCurrentGame(viewer);
                        if (currentGame != null) {
                            showPlayerStats(viewer, clickedPlayer, currentGame);
                        }
                    }
                }
            }
        }
    }

    private void showPlayerStats(Player viewer, Player target, Minigame game) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(target);
        MinigameData gameData = playerData.getMinigameData(game.getCOMMAND_MAIN_NAME(), game.isTeamGame());

        viewer.sendMessage(Prefix.SERVER + "=== " + target.getName() + "의 " + game.getDisplayGameName() + " 통계 ===");
        viewer.sendMessage(Prefix.SERVER + "총 게임 수: " + gameData.getTotalGames());
        double score = plugin.getRankingManager().getPlayerScore(game.getCOMMAND_MAIN_NAME(), target.getName());
        viewer.sendMessage(Prefix.SERVER + "점수: " + String.format("%.2f", score));

        if (game.isTeamGame()) {
            viewer.sendMessage(Prefix.SERVER + "승리: " + gameData.getWins());
            viewer.sendMessage(Prefix.SERVER + "패배: " + gameData.getLosses());
            viewer.sendMessage(Prefix.SERVER + "무승부: " + gameData.getDraws());
            double winRate = gameData.getTotalGames() > 0 ? (double) gameData.getWins() / gameData.getTotalGames() * 100 : 0;
            viewer.sendMessage(Prefix.SERVER + "승률: " + String.format("%.2f%%", winRate));
        } else {
            for (int i = 1; i <= 10; i++) {
                viewer.sendMessage(Prefix.SERVER.toString() + i + "등: " + gameData.getRankCount(i) + "회");
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
        Player player = (Player) event.getPlayer();
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());

        if (title.startsWith(MAIN_GUI_TITLE)) {
            playerGUIs.remove(player.getUniqueId()); //depre
        }

        if (event.getView().getTitle().equals("참가 중인 플레이어")) {
            BukkitTask task = updateTasks.remove(player);
            if (task != null) {
                task.cancel();
            }
        }
    }


    private boolean hasCustomName(ItemStack item, String name) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            Component displayName = item.getItemMeta().displayName();
            String plainTextDisplayName = PlainTextComponentSerializer.plainText().serialize(displayName);
            return plainTextDisplayName.contains(name);
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
                    Component titleComponent = player.getOpenInventory().title();
                    if (titleComponent instanceof TextComponent textComponent && textComponent.content().startsWith(MAIN_GUI_TITLE)) {
                        updateGUI(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void updateGUI(Player player) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        for (int i = 0; i < plugin.getMinigameList().size(); i++) {
            Minigame minigame = plugin.getMinigameList().get(i);
            ItemStack icon = createMinigameIcon(minigame);
            gui.setItem(i, icon);
        }
    }


    private void showRankings(Player player) {
        Inventory rankingGUI = Bukkit.createInventory(null, 54, Component.text("게임 랭킹").color(NamedTextColor.BLACK));

        for (Minigame minigame : plugin.getMinigameList()) {
            List<Map.Entry<String, Double>> topPlayers = plugin.getRankingManager().getTopPlayers(minigame.getCOMMAND_MAIN_NAME(), 10);

            ItemStack icon = new ItemStack(Material.GOLD_BLOCK);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Component.text(minigame.getDisplayGameName() + " 랭킹").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            List<Component> lore = new ArrayList<>();

            for (int i = 0; i < topPlayers.size(); i++) {
                Map.Entry<String, Double> entry = topPlayers.get(i);
                lore.add(Component.text(String.format("%d. %s - %.2f점", i + 1, entry.getKey(), entry.getValue())).color(NamedTextColor.YELLOW));
            }

            meta.lore(lore);
            icon.setItemMeta(meta);
            rankingGUI.addItem(icon);
        }

        player.openInventory(rankingGUI);
    }


    private void showPlayerStats(Player player) {
        Inventory statsGUI = Bukkit.createInventory(null, 54, Component.text("게임 통계", NamedTextColor.BLACK));

        for (Minigame minigame : plugin.getMinigameList()) {
            PlayerData playerData = plugin.getDataManager().getPlayerData(player);
            MinigameData data = playerData.getMinigameData(minigame.getCOMMAND_MAIN_NAME(), minigame.isTeamGame());

            ItemStack icon = new ItemStack(Material.BOOK);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Component.text(minigame.getDisplayGameName(), NamedTextColor.GOLD));

            List<Component> lore = new ArrayList<>();

            if (data.isTeamGame()) {
                int wins = data.getWins();
                int losses = data.getLosses();
                int draws = data.getDraws();
                double winRate = data.getWinRate() * 100;
                lore.add(Component.text("승리: ", NamedTextColor.YELLOW).append(Component.text(wins, NamedTextColor.GREEN)));
                lore.add(Component.text("패배: ", NamedTextColor.YELLOW).append(Component.text(losses, NamedTextColor.RED)));
                lore.add(Component.text("무승부: ", NamedTextColor.YELLOW).append(Component.text(draws, NamedTextColor.GRAY)));
                lore.add(Component.text("승률: ", NamedTextColor.YELLOW).append(Component.text(String.format("%.2f%%", winRate), NamedTextColor.AQUA)));
            } else {
                for (int i = 1; i <= 10; i++) {
                    lore.add(Component.text(i + "등: ", NamedTextColor.YELLOW).append(Component.text(data.getRankCount(i) + "회", NamedTextColor.GREEN)));
                }
            }

            meta.lore(lore);
            icon.setItemMeta(meta);
            statsGUI.addItem(icon);
        }

        player.openInventory(statsGUI);
    }
}