package kr.egsuv;

import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.CommandTabCompleter;
import kr.egsuv.commands.commandList.*;
import kr.egsuv.config.ConfigManager;
import kr.egsuv.config.MinigameConfig;
import kr.egsuv.data.BlockRestoreManager;
import kr.egsuv.data.DataManager;
import kr.egsuv.listeners.*;
import kr.egsuv.commands.CommandManager;
import kr.egsuv.minigames.*;
import kr.egsuv.minigames.games.firsthit.FirstHitGame;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import kr.egsuv.minigames.games.spleef.SpleefGame;
import kr.egsuv.minigames.games.teamdeathmatch.TeamDeathmatchGame;
import kr.egsuv.ranking.RankingManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;


public final class EGServerMain extends JavaPlugin implements Listener {

    private static EGServerMain instance;
    private CommandManager commandManager;
    private MinigameGui miniGamesGui;
    private ConfigManager configManager;
    private SpawnCommand spawnCommand;
    private RankingManager rankingManager;
    private DataManager dataManager;
    private MinigameItems minigameItems;
    private MinigamePenaltyManager minigamePenaltyManager;
    private MinigameConfig minigameConfig;

    // 미니게임
    private FirstHitGame firstHitGame;
    private TeamDeathmatchGame teamDeathmatchGame;
    private SpleefGame spleefGame;
    private WarOfGodGame warOfGodGame;

    // 맵 복구
    private BlockRestoreManager blockRestoreManager;

    private Map<UUID, String> playerList = new HashMap<>();
    private List<Minigame> minigameList = new ArrayList<>();

    // ANSI 색상 코드
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_BOLD = "\u001B[1m";

    @Override
    public void onEnable() {
        for (Player onlinePlayer : getServer().getOnlinePlayers()) {
            playerList.put(onlinePlayer.getUniqueId(), "로비");
        }
        instance = this;
        commandManager = new CommandManager();
        miniGamesGui = new MinigameGui();
        configManager = new ConfigManager();
        rankingManager = new RankingManager();
        dataManager = new DataManager();
        minigameItems = new MinigameItems();
        minigamePenaltyManager = new MinigamePenaltyManager();
        spawnCommand = new SpawnCommand(miniGamesGui);

        blockRestoreManager = new BlockRestoreManager();
        // BlockRestoreManager 초기화 후 즉시 데이터 로드
        Bukkit.getScheduler().runTaskLater(this, () -> {
            blockRestoreManager.loadRestoreData();
            getLogger().info("BlockRestoreManager 초기화 상태:");
            getLogger().info("gameMapRegions: " + blockRestoreManager.getGameMapRegions());
            getLogger().info("gameMapCenters: " + blockRestoreManager.getGameMapCenters());
        }, 40L); // 1초 후에 실행 (서버가 완전히 로드된 후)

        MinigamePenaltyManager.startCleanupTask(this);

        initializeMinigames();

        registerListeners();
        registerCommands();

        getLogger().info(ANSI_GREEN + ANSI_BOLD + "EG 서버 플러그인 활성화" + ANSI_RESET);
    }

    private void initializeMinigames() {
        firstHitGame = new FirstHitGame(this, minigameItems, "fth", 2, 8, "§a선빵 게임§r", true);
        teamDeathmatchGame = new TeamDeathmatchGame(this, minigameItems, "tdm", 2, 12, "§c팀 데스매치§r", true, TeamType.DUO, 2, false, false);
        spleefGame = new SpleefGame(this, minigameItems, "spf", 2, 10, "§b스플리프", true);
        warOfGodGame = new WarOfGodGame(this, minigameItems, "wog", 2, 8, "§6신들의 전쟁", true, TeamType.SQUAD, 2, false, true);

        minigameList.add(firstHitGame);
        minigameList.add(teamDeathmatchGame);
        minigameList.add(spleefGame);
        minigameList.add(warOfGodGame);

        for (Minigame minigame : minigameList) {
            minigame.setBlockRestoreManager(blockRestoreManager);
        }
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("모든 스케줄러 작업 취소 및 서버 종료 중...");

        for (Player onlinePlayer : getServer().getOnlinePlayers()) {
            teleportToSpawn(onlinePlayer);
            onlinePlayer.sendMessage(Prefix.SERVER + "§c서버가 리로딩 중입니다. 모든 게임이 중단됩니다.");
            playerList.put(onlinePlayer.getUniqueId(), "로비");
            onlinePlayer.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            onlinePlayer.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        }

        for (Minigame minigame : minigameList) {
            try {
                minigame.forceEnd();
            } catch (Exception e) {
                getLogger().severe("게임 '" + minigame.getDisplayGameName() + "' 종료 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (blockRestoreManager != null) {
            blockRestoreManager.forceRestoreAllMaps();
            blockRestoreManager.saveRestoreData();
        }

        getLogger().info("서버가 안전하게 종료되었습니다.");
        minigameList.clear();
        getLogger().info(ANSI_GREEN + ANSI_BOLD + "EG 서버 플러그인 비활성화" + ANSI_RESET);
    }

    public static EGServerMain getInstance() {
        return instance;
    }


    private void registerCommands() {
        // 명령어 등록

        commandManager.registerCommand(spawnCommand, "스폰", "spawn");
        commandManager.registerCommand(new TimeCommand(), "time", "시간");
        commandManager.registerCommand(new MegaphoneCommand(), "확성기", "메아리", "megaphone");
        commandManager.registerCommand(new MinigameCommand(), "minigame", "game");

        // TabCompleter 등록
        CommandTabCompleter commandTabCompleter = new CommandTabCompleter();
        commandTabCompleter.setCompletions("스폰", "spawn", "시간", "time", "확성기", "메아리", "megaphone");
        registerTabCompleter(commandTabCompleter);
    }

    private void registerTabCompleter(TabCompleter tabCompleter) {
        for (String command : ((CommandTabCompleter) tabCompleter).getCompletions()) {
            getCommand(command).setTabCompleter(tabCompleter);
        }
    }

    public void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        List<Listener> listeners = Arrays.asList(
                // 리스너 추가
                new ChatListener(),
                new CommandListener(commandManager),
                new PlayerJoinListener(new SpawnCommand(miniGamesGui), minigameList),
                new PlayerQuitListener(minigameList),
                new PlayerDamageListener(),
                new NoDropOrMoveListener(),
                new CustomGUIListener(),
                new PlayerDeathListener()
        );

        for (Listener listener : listeners) {
            pluginManager.registerEvents(listener, this);
            getLogger().info("이벤트 등록: " + listener.getClass().getName());
        }
    }

    public void safelyTeleportPlayer(Player player, Location destination) {
        player.teleport(destination);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            player.teleport(player.getLocation());
        }, 1L);
    }

    public void broadcastToServer(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    // 현재 플레이어 위치 설정
    public void setPlayerList(Player player, String location) {
        playerList.put(player.getUniqueId(), location);
    }

    public Map<UUID, String> getPlayerList() {
        return playerList;
    }

    public String getPlayerListLocation(Player player) {
        return playerList.get(player.getUniqueId());
    }

    public ConfigManager getSpawnConfigManager() {
        return configManager;
    }

    public Location getLobbyLocation() {
        return getSpawnConfigManager().getSpawnLocation();
    }

    public boolean initPlayer(Player player) {
        return spawnCommand.initPlayer(player);
    }

    public void teleportToSpawn(Player player) {
        spawnCommand.teleportToSpawn(player);
    }

    public FirstHitGame getFirstHitGame() {
        return firstHitGame;
    }

    public List<Minigame> getMinigameList() {
        return minigameList;
    }

    public RankingManager getRankingManager() {
        return rankingManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public TeamDeathmatchGame getTeamDeathmatchGame() { return teamDeathmatchGame; }

    //해당 게임 (commandName) 의 미니게임 인스턴스 반환
    public Minigame getMinigameByName(String commandName) {
        for (Minigame minigame : minigameList) {
            if (minigame.getCOMMAND_MAIN_NAME().equalsIgnoreCase(commandName)) {
                return minigame;
            }
        }
        return null; // 해당 이름의 미니게임이 없을 경우
    }

    public Minigame getCurrentGame(Player player) {
        String playerStatus = playerList.get(player.getUniqueId());

        // 플레이어가 게임 중이거나 게임 로비에 있는 경우
        if (playerStatus != null && !playerStatus.equals("로비")) {
            for (Minigame minigame : minigameList) {
                if (minigame.getCOMMAND_MAIN_NAME().equalsIgnoreCase(playerStatus) ||
                        minigame.isPlayerInGameLobby(player)) {
                    return minigame;
                }
            }
        }

        return null; // 플레이어가 게임 중이 아님
    }

    // 해당 미니게임의 Config 찾기
    public MinigameConfig getMinigameConfig(String gameName) {
        Minigame minigame = getMinigameByName(gameName);
        if (minigame != null) {
            return minigame.getConfig();
        }
        return null;
    }


    public BlockRestoreManager getBlockRestoreManager() {
        return blockRestoreManager;
    }

    public void reloadBlockRestoreData() {
        if (blockRestoreManager != null) {
            blockRestoreManager.reloadData();
        }
    }

}
