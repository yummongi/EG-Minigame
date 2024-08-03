package kr.egsuv;

import kr.egsuv.commands.CommandTabCompleter;
import kr.egsuv.config.ConfigManager;
import kr.egsuv.commands.commandList.MegaphoneCommand;
import kr.egsuv.listeners.ChatListener;
import kr.egsuv.listeners.CommandListener;
import kr.egsuv.commands.CommandManager;
import kr.egsuv.commands.commandList.SpawnCommand;
import kr.egsuv.commands.commandList.TimeCommand;
import kr.egsuv.listeners.PlayerJoinListener;
import kr.egsuv.listeners.PlayerQuitListener;
import kr.egsuv.minigames.MiniGamesGui;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;


public final class EGServerMain extends JavaPlugin implements Listener {

    private static EGServerMain instance;
    private CommandManager commandManager;
    private MiniGamesGui miniGamesGui;
    private ConfigManager configManager;

    private Map<UUID, String> playerList = new HashMap<>();


    // ANSI 색상 코드
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_BOLD = "\u001B[1m";

    @Override
    public void onEnable() {

        instance = this;
        commandManager = new CommandManager();
        miniGamesGui = new MiniGamesGui();
        configManager = new ConfigManager();

        //리스너 등록
        registerListeners();
        //커맨드 등록
        registerCommands();
        getLogger().info(ANSI_GREEN + ANSI_BOLD + "EG 서버 플러그인 활성화" + ANSI_RESET);

    }

    @Override
    public void onDisable() {
        getLogger().info(ANSI_GREEN + ANSI_BOLD + "EG 서버 플러그인 비활성화" + ANSI_RESET);

    }

    public static EGServerMain getInstance() {
        return instance;
    }

    private void registerCommands() {
        // 명령어 등록
        commandManager.registerCommand(new SpawnCommand(miniGamesGui), "스폰", "spawn");
        commandManager.registerCommand(new TimeCommand(), "time", "시간");
        commandManager.registerCommand(new MegaphoneCommand(), "확성기", "메아리", "megaphone");

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
                new PlayerQuitListener()
        );

        for (Listener listener : listeners) {
            pluginManager.registerEvents(listener, this);
            getLogger().info("이벤트 등록: " + listener.getClass().getName());
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
}
