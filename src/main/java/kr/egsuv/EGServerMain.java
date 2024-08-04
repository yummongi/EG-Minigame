package kr.egsuv;

import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.CommandTabCompleter;
import kr.egsuv.commands.commandList.*;
import kr.egsuv.config.ConfigManager;
import kr.egsuv.data.DataManager;
import kr.egsuv.listeners.*;
import kr.egsuv.commands.CommandManager;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameGui;
import kr.egsuv.minigames.games.FirstHitGame;
import kr.egsuv.minigames.games.TeamDeathmatchGame;
import kr.egsuv.ranking.RankingManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
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

    // 미니게임
    private FirstHitGame firstHitGame;
    private TeamDeathmatchGame teamDeathmatchGame;

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
        spawnCommand = new SpawnCommand(miniGamesGui);
        rankingManager = new RankingManager();
        dataManager = new DataManager();

        // 미니게임 추가
        firstHitGame = new FirstHitGame(this, "firsthit", 2, 8, "§7[ §a선빵 게임 §7]", false);
        minigameList.add(firstHitGame);

        // TeamDeathmatchGame 추가
        teamDeathmatchGame = new TeamDeathmatchGame(this, "teamdeathmatch", 2, 16, "§7[ §c팀 데스매치 §7]");
        minigameList.add(teamDeathmatchGame);

        //리스너 등록
        registerListeners();
        //커맨드 등록
        registerCommands();

        getLogger().info(ANSI_GREEN + ANSI_BOLD + "EG 서버 플러그인 활성화" + ANSI_RESET);

    }

    @Override
    public void onDisable() {
        for (Minigame minigame : minigameList) {
            minigame.getPlayers().clear();
            BossBar bossBar = minigame.getBossBar();
            BossBar lobbyBossBar = minigame.getLobbyBossBar();
            if (bossBar != null) {
                bossBar.removeAll();
            }
            if (lobbyBossBar != null) {
                lobbyBossBar.removeAll();
            }
        }

        for (Player onlinePlayer : getServer().getOnlinePlayers()) {
            teleportToSpawn(onlinePlayer);
            onlinePlayer.sendMessage(Prefix.SERVER + "§c서버가 리로딩 중입니다. 모든 게임이 중단됩니다.");
            playerList.put(onlinePlayer.getUniqueId(), "로비");
            onlinePlayer.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

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
        commandManager.registerCommand(new FirstHitCommand(firstHitGame), "firsthit");
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
                new PlayerJoinListener(new SpawnCommand(miniGamesGui)),
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
}
