package kr.egsuv.minigames;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.config.MinigameConfig;
import kr.egsuv.data.MinigameData;
import kr.egsuv.data.PlayerData;
import kr.egsuv.util.ItemUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public abstract class Minigame {

    protected final EGServerMain plugin;
    protected final String COMMAND_MAIN_NAME;
    protected final int MIN_PLAYER;
    protected final int MAX_PLAYER;
    protected final String DISPLAY_GAME_NAME;
    protected final ItemStack HELPER_ITEM;
    protected final boolean isTeamGame;

    protected int current_player;
    protected MinigameState state = MinigameState.WAITING;
    protected Set<Player> players = new HashSet<>();
    protected Map<Player, Integer> scores = new HashMap<>();

    protected BukkitTask countdownTask;
    protected static final int DEFAULT_COUNTDOWN_TIME = 15;
    private int countdownTime = DEFAULT_COUNTDOWN_TIME;
    protected int gameTimeLimit;

    // 탈주 관련
    public static final long RECONNECT_WINDOW = 300000; // 재입장 가능 5분
    private static final long REJOIN_COOLDOWN = 30000; // 재입장 후 관전 30초

    protected boolean blockBreakAllowed = false;
    protected boolean blockPlaceAllowed = false;
    protected boolean itemDropAllowed = false;
    protected boolean itemPickupAllowed = false;
    protected boolean itemMoveAllowed = false;

    protected BossBar lobbyBossBar;
    protected BossBar bossBar;

    protected MinigameConfig config;

    // 팀 관리
    protected Map<Player, String> playerTeams = new HashMap<>();
    protected TeamType teamType;
    protected int numberOfTeams;
    protected Map<String, List<Player>> teams;

    //팀 채팅
    private Map<Player, Boolean> teamChatEnabled = new HashMap<>();

    // 탈주
    private Set<UUID> disconnectedPlayers = new HashSet<>();

    // kda
    private Map<Player, Integer> kills = new HashMap<>();
    private Map<Player, Integer> deaths = new HashMap<>();
    private Map<String, Integer> teamKills = new HashMap<>();
    private Map<String, Integer> teamDeaths = new HashMap<>();

    // 팀 색상 정의
    protected static final Map<String, ChatColor> TEAM_COLORS = new HashMap<>() {{
        put("red", ChatColor.RED);
        put("blue", ChatColor.BLUE);
        put("green", ChatColor.GREEN);
        put("yellow", ChatColor.YELLOW);
    }};

    protected Scoreboard scoreboard;
    protected Objective objective;
    protected Map<String, Team> scoreboardTeams = new HashMap<>();

    protected MinigameItems minigameItems;
    // 개인전 생성자
    public Minigame(EGServerMain plugin, MinigameItems minigameItems, String commandMainName, int MIN_PLAYER, int MAX_PLAYER, String displayGameName) {
        this.plugin = plugin;
        this.COMMAND_MAIN_NAME = commandMainName;
        this.MIN_PLAYER = MIN_PLAYER;
        this.MAX_PLAYER = MAX_PLAYER;
        this.DISPLAY_GAME_NAME = displayGameName;
        this.config = new MinigameConfig(plugin, commandMainName);
        this.teamType = TeamType.SOLO; // teamType을 여기서 초기화합니다.
        this.config.setTeamType(teamType);
        this.config.setNumberOfTeams(numberOfTeams);
        this.isTeamGame = false;
        this.numberOfTeams = MAX_PLAYER;
        this.teams = new HashMap<>();
        for (int i = 1; i <= MAX_PLAYER; i++) {
            teams.put("개인" + i, new ArrayList<>());
        }

        // 미니게임 관련 인스턴스 생성
        this.minigameItems = minigameItems;

        this.HELPER_ITEM = ItemUtils.createItem(Material.ENCHANTED_BOOK, 1, "§7[ §f게임 §a튜토리얼 §f읽기 §7]",
                "§7§l| §f우클릭 시 게임 하는 방법을 알아볼 수 있습니다.");
    }

    // 팀 게임용 생성자
    public Minigame(EGServerMain plugin, MinigameItems minigameItems, String commandMainName, int MIN_PLAYER, int MAX_PLAYER, String displayGameName, boolean isTeamGame, TeamType teamType, int numberOfTeams) {
        this.plugin = plugin;
        this.COMMAND_MAIN_NAME = commandMainName;
        this.MIN_PLAYER = MIN_PLAYER;
        this.MAX_PLAYER = MAX_PLAYER;
        this.DISPLAY_GAME_NAME = displayGameName;
        this.isTeamGame = isTeamGame;
        this.config = new MinigameConfig(plugin, commandMainName);

        this.teamType = teamType; // teamType을 여기서 초기화합니다.
        this.config.setTeamType(teamType);
        this.config.setNumberOfTeams(numberOfTeams);

        // 미니게임 관련 인스턴스 생성
        this.minigameItems = new MinigameItems();

        this.numberOfTeams = numberOfTeams;
        this.teams = new HashMap<>();
        for (int i = 1; i <= numberOfTeams; i++) {
            teams.put("팀" + i, new ArrayList<>());
        }

        this.HELPER_ITEM = ItemUtils.createItem(Material.ENCHANTED_BOOK, 1, "§7[ §f게임 §a튜토리얼 §f읽기 §7]",
                "§7§l| §f우클릭 시 게임 하는 방법을 알아볼 수 있습니다.");
    }

    // 팀 할당 및 스코어보드 설정을 위한 공통 메서드
    protected void setupTeamsAndScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("game", "dummy");
        objective.setDisplayName(ChatColor.GOLD + getDisplayGameName());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        if (isTeamGame) {
            for (String teamName : teams.keySet()) {
                Team team = scoreboard.registerNewTeam(teamName);
                ChatColor teamColor = TEAM_COLORS.getOrDefault(teamName, ChatColor.WHITE);
                team.setColor(teamColor);
                team.setPrefix(teamColor + "[" + teamName.toUpperCase() + "] ");
                scoreboardTeams.put(teamName, team);

                for (Player player : teams.get(teamName)) {
                    team.addEntry(player.getName());
                    player.setPlayerListName(teamColor + player.getName());
                    player.setDisplayName(teamColor + player.getName() + ChatColor.RESET);
                    giveColoredArmor(player, teamName);
                }
                objective.getScore(teamName.toUpperCase()).setScore(0);
            }
        } else {
            for (Player player : players) {
                objective.getScore(player.getName()).setScore(0);
            }
        }

        for (Player player : players) {
            player.setScoreboard(scoreboard);
        }

        initializeScores();
    }

    protected void initializeScores() {
        for (Player player : players) {
            scores.put(player, 0);
        }
    }

    // 팀 채팅
    public void toggleTeamChat(Player player) {
        if (!isTeamGame || !players.contains(player) || state != MinigameState.IN_PROGRESS) {
            player.sendMessage(Prefix.SERVER + "팀 채팅은 팀 게임 진행 중에만 사용할 수 있습니다.");
            return;
        }

        String team = getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(Prefix.SERVER + "팀에 속해있지 않아 팀 채팅을 사용할 수 없습니다.");
            return;
        }

        boolean current = teamChatEnabled.getOrDefault(player, false);
        teamChatEnabled.put(player, !current);
        player.sendMessage(Prefix.SERVER + "팀 채팅이 " + (!current ? "활성화" : "비활성화") + "되었습니다.");
    }


    public void broadcastToTeam(String message, String team) {
        for (Player player : teams.get(team)) {
            player.sendMessage(message);
        }
    }

    public void handleChat(Player player, String message) {
        if (!players.contains(player) || state != MinigameState.IN_PROGRESS) {
            return; // 게임에 참여하지 않았거나 게임이 진행 중이 아닌 경우 무시
        }

        String team = getPlayerTeam(player);
        if (isTeamGame && teamChatEnabled.getOrDefault(player, false) && team != null) {
            broadcastToTeam(ChatColor.GREEN + "[팀 " + team + "] " + player.getName() + ": " + message, team);
        } else {
            broadcastToPlayers(ChatColor.WHITE + player.getName() + ": " + message);
        }
    }

    // 색상이 있는 가죽 갑옷을 지급하는 메서드
    protected void giveColoredArmor(Player player, String teamName) {
        Color armorColor = getArmorColor(teamName);
        player.getInventory().setHelmet(createColoredArmor(Material.LEATHER_HELMET, armorColor));
        player.getInventory().setChestplate(createColoredArmor(Material.LEATHER_CHESTPLATE, armorColor));
        player.getInventory().setLeggings(createColoredArmor(Material.LEATHER_LEGGINGS, armorColor));
        player.getInventory().setBoots(createColoredArmor(Material.LEATHER_BOOTS, armorColor));
    }

    // 팀에 따른 갑옷 색상 반환
    private Color getArmorColor(String teamName) {
        switch (teamName.toLowerCase()) {
            case "red": return Color.RED;
            case "blue": return Color.BLUE;
            case "green": return Color.GREEN;
            case "yellow": return Color.YELLOW;
            default: return Color.WHITE;
        }
    }

    // 색상이 있는 가죽 갑옷 아이템 생성
    private ItemStack createColoredArmor(Material material, Color color) {
        ItemStack armor = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        meta.setColor(color);
        armor.setItemMeta(meta);
        return armor;
    }

    // 스코어 업데이트 메서드
    protected void updateScore(String teamOrPlayerName, int score) {
        if (isTeamGame) {
            objective.getScore(teamOrPlayerName.toUpperCase()).setScore(score);
        } else {
            objective.getScore(teamOrPlayerName).setScore(score);
        }
    }




    // 게임 규칙 설정 메소드
    protected void setGameRules(boolean blockBreakAllowed, boolean blockPlaceAllowed, boolean itemDropAllowed, boolean itemPickupAllowed, boolean itemMoveAllowed) {
        this.blockBreakAllowed = blockBreakAllowed;
        this.blockPlaceAllowed = blockPlaceAllowed;
        this.itemDropAllowed = itemDropAllowed;
        this.itemPickupAllowed = itemPickupAllowed;
        this.itemMoveAllowed = itemMoveAllowed;
    }

    protected void recordGameResult() {
        List<Map.Entry<Player, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        for (Player player : players) {
            PlayerData playerData = plugin.getDataManager().getPlayerData(player);
            MinigameData gameData = playerData.getMinigameData(COMMAND_MAIN_NAME, isTeamGame);
            gameData.incrementTotalGames();
        }

        if (isTeamGame) {
            recordTeamGameResult(sortedScores);
        } else {
            recordIndividualGameResult(sortedScores);
        }
    }

    private void recordTeamGameResult(List<Map.Entry<Player, Integer>> sortedScores) {
        // 팀전 결과 기록 (예: 1등 팀은 승리, 나머지는 패배)
        for (int i = 0; i < sortedScores.size(); i++) {
            Player player = sortedScores.get(i).getKey();
            if (i == 0) {
                recordWin(player);
            } else {
                recordLoss(player);
            }
        }
    }

    private void recordIndividualGameResult(List<Map.Entry<Player, Integer>> sortedScores) {
        // 개인전 결과 기록 (1-10등까지 기록)
        for (int i = 0; i < Math.min(10, sortedScores.size()); i++) {
            Player player = sortedScores.get(i).getKey();
            recordRank(player, i + 1);
        }
    }

    protected void recordWin(Player player) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(player);
        playerData.getMinigameData(COMMAND_MAIN_NAME, true).addWin();
        plugin.getDataManager().savePlayerData(player);
    }

    protected void recordLoss(Player player) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(player);
        playerData.getMinigameData(COMMAND_MAIN_NAME, true).addLoss();
        plugin.getDataManager().savePlayerData(player);
    }

    protected void recordRank(Player player, int rank) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(player);
        playerData.getMinigameData(COMMAND_MAIN_NAME, false).addRank(rank);
        plugin.getDataManager().savePlayerData(player);
    }
    // 플레이어의 팀을 반환하는 메소드
    protected String getPlayerTeam(Player player) {
        return playerTeams.getOrDefault(player, "");
    }

    // 플레이어를 팀에 할당하는 메소드
    protected void assignPlayerToTeam(Player player, String team) {
        playerTeams.put(player, team);
        ChatColor teamColor = TEAM_COLORS.getOrDefault(team, ChatColor.WHITE);
        player.setPlayerListName(teamColor + player.getName());
        player.setDisplayName(teamColor + player.getName() + ChatColor.RESET);
    }

    // 팀 게임 시작 시 플레이어들을 팀에 할당하는 메소드
    protected void assignTeams() {
        List<Player> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        if (teamType == TeamType.SOLO) {
            for (int i = 0; i < shuffledPlayers.size(); i++) {
                Player player = shuffledPlayers.get(i);
                String playerSlot = "개인" + (i + 1);
                assignPlayerToTeam(player, playerSlot);
            }
        } else {
            int playersPerTeam = teamType.getPlayersPerTeam();
            int teamIndex = 1;
            for (int i = 0; i < shuffledPlayers.size(); i++) {
                Player player = shuffledPlayers.get(i);
                String teamName = teamType.name().toLowerCase() + teamIndex;
                assignPlayerToTeam(player, teamName);

                if ((i + 1) % playersPerTeam == 0) {
                    teamIndex++;
                }
            }
        }
    }
    public boolean canJoinGame(Player player) {
        if (state == MinigameState.ENDING) {
            player.sendMessage(Prefix.SERVER + "게임이 종료 중이어서 참여할 수 없습니다.");
            return false;
        }

        if (state == MinigameState.IN_PROGRESS) {
            // 게임 진행 중에는 탈주한 플레이어만 재참여 가능
            if (MinigamePenaltyManager.wasPlayerInGame(player.getUniqueId(), COMMAND_MAIN_NAME)) {
                return true;
            } else {
                player.sendMessage(Prefix.SERVER + "게임이 이미 진행 중이어서 새로 참여할 수 없습니다.");
                return false;
            }
        }

        // 그 외의 경우 (WAITING, STARTING)에는 일반적인 참여 규칙 적용
        return MinigamePenaltyManager.canJoinGame(player, COMMAND_MAIN_NAME);
    }
    // 게임 입장
    public void joinGame(Player player) {
        if (!canJoinGame(player)) {
            return;
        }

        if (state == MinigameState.DISABLED) {
            player.sendMessage(Prefix.SERVER + DISPLAY_GAME_NAME + "§r 게임이 현재 §c점검중§f입니다.");
        }
        if (state != MinigameState.WAITING && state != MinigameState.STARTING) {
            player.sendMessage(Prefix.SERVER + DISPLAY_GAME_NAME + "§r 게임이 이미 진행 중입니다.");
            return;
        }

        if (players.contains(player)) {
            player.sendMessage(Prefix.SERVER + "§r이미 게임에 참여 중입니다.");
            return;
        }

        if (!plugin.getPlayerListLocation(player).equals("로비")) {
            player.sendMessage(Prefix.SERVER + "이미 다른 게임에 참여 중입니다.");
            return;
        }

        if (current_player >= MAX_PLAYER) {
            player.sendMessage(Prefix.SERVER + "§r현재 " + DISPLAY_GAME_NAME + "§r 게임이 꽉 찼습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String teamToJoin = findTeamToJoin();
        if (teamToJoin == null) {
            player.sendMessage(Prefix.SERVER + "§r현재 참여 가능한 " + (isTeamGame ? "§r팀 자리" : "§r슬롯") + "가 없습니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        // 로비에서 게임로비로 위치 변경
        setPlayerListLocation(player, "게임로비");

        teams.get(teamToJoin).add(player);
        current_player++;
        players.add(player);
        assignPlayerToTeam(player, teamToJoin);
        updateLobbyBossBar();

        // 페널티 및 재접속 정보 초기화
        UUID playerId = player.getUniqueId();
        MinigamePenaltyManager.clearPlayerData(player.getUniqueId());

        broadcastToPlayers(Prefix.SERVER + player.getName() + "님이 " + DISPLAY_GAME_NAME + " 게임의 " +
                (isTeamGame ? teamToJoin + " 팀 자리" : (teamToJoin + " 번 슬롯")) + "에 입장하셨습니다. (" +
                current_player + "/" + MIN_PLAYER + ")");

        teleportToGameLobby(player);
        preparePlayerForGame(player);

        if (canStartGame() && state == MinigameState.WAITING) {
            startCountdown();
        } else {
            sendTeamBalanceInfo(player);
        }
    }

    protected void startCountdown() {
        if (state == MinigameState.STARTING) {
            return;
        }

        if (!canStartGame()) {
            broadcastToPlayers(Prefix.SERVER + "§r게임을 시작하기 위한 조건이 충족되지 않았습니다.");
            return;
        }

        state = MinigameState.STARTING;

        if (countdownTask != null) {
            countdownTask.cancel();
        }

        countdownTime = DEFAULT_COUNTDOWN_TIME;

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!canStartGame()) {
                    cancel();
                    broadcastToPlayers(Prefix.SERVER + "§c플레이어 수 부족§r으로 §c카운트다운§r이 취소되었습니다.");
                    state = MinigameState.WAITING;
                    return;
                }

                if (countdownTime > 0) {
                    // broadcastToServer 조건
                    if (countdownTime == DEFAULT_COUNTDOWN_TIME / 2 || countdownTime == 10 || countdownTime == 60) {
                        broadcastToServer(Prefix.SERVER.toString() + countdownTime + "§r초 후 " + DISPLAY_GAME_NAME + "§r 게임이 시작됩니다.");
                    }

                    // broadcastToPlayers 조건
                    if (countdownTime <= 5) {
                        broadcastToPlayers(Prefix.SERVER.toString() + countdownTime + "§r초 후 " + DISPLAY_GAME_NAME + "§r 게임이 시작됩니다.");
                    }
                    countdownTime--;
                } else {
                    cancel();
                    startGame();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    protected String findTeamToJoin() {
        if (teamType == TeamType.SOLO) {
            for (Map.Entry<String, List<Player>> entry : teams.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    return entry.getKey();
                }
            }
        } else {
            int minPlayers = Integer.MAX_VALUE;
            String teamToJoin = null;
            for (Map.Entry<String, List<Player>> entry : teams.entrySet()) {
                if (entry.getValue().size() < teamType.getPlayersPerTeam() && entry.getValue().size() < minPlayers) {
                    minPlayers = entry.getValue().size();
                    teamToJoin = entry.getKey();
                }
            }
            return teamToJoin;
        }
        return null; // 모든 팀이 꽉 찼거나 모든 개인 슬롯이 찼을 경우
    }

    private boolean canStartGame() {
        if (current_player < MIN_PLAYER) {
            return false;
        }

        if (teamType == TeamType.SOLO) {
            return current_player >= MIN_PLAYER;
        } else {
            int fullTeams = 0;
            int minPlayersPerTeam = teamType.getPlayersPerTeam();
            for (List<Player> team : teams.values()) {
                if (team.size() >= minPlayersPerTeam) {
                    fullTeams++;
                }
            }
            return fullTeams >= 2; // 최소 2개 팀이 조건을 만족해야 게임 시작 가능
        }
    }



    private void sendTeamBalanceInfo(Player player) {
        StringBuilder info = new StringBuilder("현재 게임 상황:\n");
        if (isTeamGame) {
            for (Map.Entry<String, List<Player>> entry : teams.entrySet()) {
                info.append("§a§l").append(entry.getKey()).append("§r: §e").append(entry.getValue().size()).append("§r/§c").append(teamType.getPlayersPerTeam()).append(" §r명\n");
            }
            info.append("§c게임 시작§r을 위해 §a각 팀§r에 ").append(teamType.getPlayersPerTeam()).append("§r명의 §a플레이어§r가 필요합니다.");
        } else {
            info.append("§e현재 참가자 수: ").append(current_player).append("/").append(MAX_PLAYER).append("\n");
            info.append("§r게임 시작을 위해 최소 ").append(MIN_PLAYER).append("§r명의 §a플레이어§r가 필요합니다.");
        }

        // 툴팁을 포함한 메시지 생성
        TextComponent message = new TextComponent("§e현재 게임 상황 보기");
        BaseComponent[] hoverText = new ComponentBuilder(info.toString()).create();
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));

        player.spigot().sendMessage(message);
    }
/*    // 팀 밸런스 상황 정보
    private void sendTeamBalanceInfo(Player player) {
        StringBuilder info = new StringBuilder("현재 게임 상황:\n");
        if (isTeamGame) {
            for (Map.Entry<String, List<Player>> entry : teams.entrySet()) {
                info.append("§a§l").append(entry.getKey()).append("§r: §e").append(entry.getValue().size()).append("§r/§c").append(teamType.getPlayersPerTeam()).append(" §r명\n");
            }
            info.append("§c게임 시작§r을 위해 §a각 팀§r에 ").append(teamType.getPlayersPerTeam()).append("§r명의 §a플레이어§r가 필요합니다.");
        } else {
            info.append("§e현재 참가자 수: ").append(current_player).append("/").append(MAX_PLAYER).append("\n");
            info.append("§r게임 시작을 위해 최소 ").append(MIN_PLAYER).append("§r명의 §a플레이어§r가 필요합니다.");
        }
        broadcastToPlayers(info.toString());
    }*/


    public void startGame() {
        state = MinigameState.IN_PROGRESS;
        if (lobbyBossBar != null) {
            lobbyBossBar.removeAll();
            lobbyBossBar = null;
        }
        resetKDStats();

        if (isTeamGame) {
            assignTeams();
            setupTeamsAndScoreboard();
        } else {
            setupTeamsAndScoreboard(); // 개인전용 스코어보드 설정 메서드 (구현 필요)
        }

        // 게임 시작 시 모든 플레이어의 팀 채팅 상태 초기화
        for (Player player : players) {
            teamChatEnabled.put(player, false);
        }

        for (Player player : players) {
            // 로비에서 게임명으로 위치 변경
            setPlayerListLocation(player, COMMAND_MAIN_NAME);
            initializePlayerForGame(player);
            player.teleport(getRandomSpawnLocation(player));
            player.sendMessage(Prefix.SERVER.toString() + ChatColor.YELLOW + getDisplayGameName() + " 게임이 시작 되었습니다!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            giveGameItems(player);
            // 무적 모드 해제

        }

        broadcastTitle("§6§l" + DISPLAY_GAME_NAME, "§e게임 시작!", 10, 70, 20);
        setupGameSpecificRules();
        onGameStart();
        startGameTimer();

    }

    // 비정상 종료 체크 : 파라미터
    public void endGame(boolean isAbnormalEnd) {
        if (state == MinigameState.ENDING) return; // 이미 종료 프로세스가 시작되었다면 중복 실행 방지

        onGameEnd();
        cancelCountdown();
        state = MinigameState.ENDING;


        if (isAbnormalEnd) {
            for (UUID playerId : disconnectedPlayers) {
                MinigamePenaltyManager.applyPenaltyForAbnormalEnd(playerId);
            }
        }

        // 모든 플레이어를 무적 상태로 만듦
        for (Player player : players) {
            player.setInvulnerable(true);
            player.sendMessage(Prefix.SERVER + " §c15초 §f후 게임이 종료됩니다.");
            teamChatEnabled.remove(player);
        }

        showFinalRanking();
        recordGameResult();

        if (bossBar != null) {
            bossBar.removeAll(); // BossBar 제거
            bossBar = null;
        }

        if (lobbyBossBar != null) {
            lobbyBossBar.removeAll();
            lobbyBossBar = null;
        }


        // 15초 후에 플레이어들을 스폰으로 보내고 게임을 리셋
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<Player> playersToTeleport = new ArrayList<>(players);

            // 모든 플레이어의 스코어보드를 기본 스코어보드로 재설정하고 무적 상태 해제
            for (Player player : new ArrayList<>(players)) {
                gameQuitPlayer(player);
                player.setInvulnerable(false);
                if (!disconnectedPlayers.contains(player.getUniqueId())) {
                    MinigamePenaltyManager.clearPlayerData(player.getUniqueId());
                }
            }

            broadcastToServer(Prefix.SERVER + DISPLAY_GAME_NAME + "§r 게임이 종료되었습니다!");
            resetGame();
            state = MinigameState.WAITING;
        }, 300L); // 15초 (20틱 * 15 = 300틱)
    }



    // 강제 실행
    public void forceStart() {
        if (state == MinigameState.WAITING || state == MinigameState.STARTING) {
            if (canStartGame()) {
                if (countdownTask != null) {
                    countdownTask.cancel();
                }
                startGame();
            } else {
                broadcastToPlayers(Prefix.SERVER + "§c게임을 시작하기 위한 조건이 충족되지 않았습니다.");
            }
        } else {
            broadcastToPlayers(Prefix.SERVER + "§c게임이 이미 진행 중입니다.");
        }
    }

    // 로비 BossBar 업데이트
    protected void updateLobbyBossBar() {
        if (state == MinigameState.WAITING || state == MinigameState.STARTING) {
            if (lobbyBossBar == null) {
                lobbyBossBar = Bukkit.createBossBar("대기 중: " + current_player + "/" + MIN_PLAYER, BarColor.BLUE, BarStyle.SOLID);
            }
            lobbyBossBar.setTitle("대기 중: " + current_player + "/" + MIN_PLAYER);
            double progress = (double) current_player / MIN_PLAYER;
            if (progress > 1.0) {
                progress = 1.0;
            }
            lobbyBossBar.setProgress(progress);
            if (current_player >= MIN_PLAYER) {
                lobbyBossBar.setColor(BarColor.GREEN);
            } else {
                lobbyBossBar.setColor(BarColor.BLUE);
            }
            for (Player player : players) {
                lobbyBossBar.addPlayer(player);
            }
        } else if (lobbyBossBar != null) {
            lobbyBossBar.removeAll();
            lobbyBossBar = null;
        }
    }

    // 랭킹 출력 및 폭죽 실행
    protected void showFinalRanking() {
        List<Map.Entry<Player, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        StringBuilder ranking = new StringBuilder("§6=== " + getDisplayGameName() + " 최종 랭킹 §6===\n");
        for (int i = 0; i < Math.min(10, sortedScores.size()); i++) {
            Map.Entry<Player, Integer> entry = sortedScores.get(i);
            ranking.append("§e").append(i + 1).append(". §f").append(entry.getKey().getName()).append(": §b").append(entry.getValue()).append("\n");
        }

        for (Player player : getPlayers()) {
            player.sendMessage(ranking.toString());
        }

        StringBuilder kdInfo = new StringBuilder("§6=== K/D 정보 ===\n");
        if (isTeamGame) {
            for (String team : teams.keySet()) {
                double teamKD = getTeamKDRatio(team);
                kdInfo.append("§e").append(team).append(": §b").append(String.format("%.2f", teamKD)).append("\n");
            }
        } else {
            for (Player player : players) {
                double playerKD = getKDRatio(player);
                kdInfo.append("§e").append(player.getName()).append(": §b").append(String.format("%.2f", playerKD)).append("\n");
            }
        }
        for (Player player : getPlayers()) {
            player.sendMessage(kdInfo.toString());
        }

        if (!sortedScores.isEmpty()) {
            Player winner = sortedScores.get(0).getKey();
            broadcastTitle("§6" + winner.getName(), "§e1위 축하합니다!", 10, 70, 20);

            // 우승자 위치에서 폭죽 발사
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                spawnFireworks(winner.getLocation(), 3);
            }, 40L); // 2초 후 폭죽 발사
        }
    }

    // 폭죽 발사 메소드
    protected void spawnFireworks(Location location, int amount) {
        for (int i = 0; i < amount; i++) {
            Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
            FireworkMeta fwm = fw.getFireworkMeta();

            Random r = new Random();
            FireworkEffect.Type type = FireworkEffect.Type.values()[r.nextInt(FireworkEffect.Type.values().length)];

            Color c1 = Color.fromRGB(r.nextInt(256), r.nextInt(256), r.nextInt(256));
            Color c2 = Color.fromRGB(r.nextInt(256), r.nextInt(256), r.nextInt(256));

            FireworkEffect effect = FireworkEffect.builder()
                    .flicker(r.nextBoolean())
                    .withColor(c1)
                    .withFade(c2)
                    .with(type)
                    .trail(r.nextBoolean())
                    .build();

            fwm.addEffect(effect);
            fwm.setPower(r.nextInt(2) + 1);

            fw.setFireworkMeta(fwm);
        }
    }

    // 게임 퇴장
    public void gameQuitPlayer(Player player) {
        if (players.remove(player)) {
            current_player--;
            plugin.setPlayerList(player, "로비");
            plugin.teleportToSpawn(player);
            player.sendMessage(Prefix.SERVER + DISPLAY_GAME_NAME + " 게임에서 나가셨습니다.");
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            kills.remove(player);
            deaths.remove(player);

            if (isTeamGame) {
                String team = getPlayerTeam(player);
                // 팀의 킬/데스 수 조정
                teamKills.put(team, teamKills.getOrDefault(team, 0) - kills.getOrDefault(player, 0));
                teamDeaths.put(team, teamDeaths.getOrDefault(team, 0) - deaths.getOrDefault(player, 0));
            }

            // 팀에서 플레이어 제거
            String team = playerTeams.remove(player);
            if (team != null) {
                teams.get(team).remove(player);
            }

            if (bossBar != null) {
                bossBar.removePlayer(player); // BossBar에서 플레이어 제거
            }

            if (lobbyBossBar != null) {
                lobbyBossBar.removePlayer(player);
            }

            removePlayerFromScoreboard(player);
            checkGameState();
            updateLobbyBossBar();
            teamChatEnabled.remove(player);
            checkTeamBalance();
        }
    }

    protected void checkTeamBalance() {
        if (isTeamGame && state == MinigameState.IN_PROGRESS) {
            Map<String, Integer> teamCounts = new HashMap<>();
            for (Map.Entry<String, List<Player>> entry : teams.entrySet()) {
                teamCounts.put(entry.getKey(), entry.getValue().size());
            }

            int minTeamSize = Collections.min(teamCounts.values());
            int totalPlayers = teamCounts.values().stream().mapToInt(Integer::intValue).sum();

            if (minTeamSize == 0) {
                // 한 팀의 모든 플레이어가 나갔을 경우
                broadcastToPlayers(Prefix.SERVER + "팀 밸런스가 무너져 게임을 종료합니다.");
                endGame(true);
            } else if (totalPlayers < MIN_PLAYER) {
                // 전체 플레이어 수가 최소 인원 미만일 경우
                broadcastToPlayers(Prefix.SERVER + "인원 부족으로 게임을 종료합니다.");
                endGame(true);
            } else if (minTeamSize < MIN_PLAYER / teams.size() / 2) {
                // 한 팀의 플레이어 수가 최소 인원의 절반 이하로 떨어졌을 경우
                broadcastToPlayers(Prefix.SERVER + "팀 밸런스를 위해 팀을 재조정합니다.");
                balanceTeams();
            }
        }
    }

    /*
    팀 재조정 동안 모든 플레이어를 일시적으로 관전 모드로 변경합니다. 이는 재조정 과정 중 플레이어 간 상호작용을 방지합니다.
    각 플레이어를 새 팀의 스폰 위치로 텔레포트합니다.
    팀이 변경된 플레이어에게만 개별적으로 메시지를 보냅니다.
    모든 재조정이 완료된 후 1초의 지연 시간을 두고 플레이어들을 서바이벌 모드로 변경하고, 체력과 배고픔을 최대로 채웁니다.
    게임 아이템을 다시 지급합니다. 이는 팀 변경으로 인한 장비 차이를 방지합니다.
     */

    private void balanceTeams() {
        Map<Player, Integer> playerScores = new HashMap<>(scores);
        Map<Player, Integer> playerKills = new HashMap<>(kills);
        Map<Player, Integer> playerDeaths = new HashMap<>(deaths);
        Map<Player, Boolean> playerTeamChatStates = new HashMap<>(teamChatEnabled);

        List<Player> allPlayers = new ArrayList<>(players);
        Collections.shuffle(allPlayers);
        Map<String, List<Player>> newTeams = new HashMap<>();
        for (String teamName : teams.keySet()) {
            newTeams.put(teamName, new ArrayList<>());
        }

        int playersPerTeam = allPlayers.size() / teams.size();
        int extraPlayers = allPlayers.size() % teams.size();
        int currentTeamIndex = 0;

        // 모든 플레이어를 잠시 관전 모드로 변경
        for (Player player : allPlayers) {
            player.setGameMode(GameMode.SPECTATOR);
        }

        for (Player player : allPlayers) {
            String teamName = (String) teams.keySet().toArray()[currentTeamIndex];
            newTeams.get(teamName).add(player);
            if (newTeams.get(teamName).size() >= playersPerTeam + (extraPlayers > 0 ? 1 : 0)) {
                currentTeamIndex = (currentTeamIndex + 1) % teams.size();
                if (extraPlayers > 0) extraPlayers--;
            }
        }

        teams = newTeams;
        for (Map.Entry<String, List<Player>> entry : teams.entrySet()) {
            for (Player player : entry.getValue()) {
                String oldTeam = getPlayerTeam(player);
                String newTeam = entry.getKey();

                assignPlayerToTeam(player, newTeam);

                // 플레이어의 점수, 킬/데스, 팀 채팅 상태 유지
                scores.put(player, playerScores.getOrDefault(player, 0));
                kills.put(player, playerKills.getOrDefault(player, 0));
                deaths.put(player, playerDeaths.getOrDefault(player, 0));
                teamChatEnabled.put(player, playerTeamChatStates.getOrDefault(player, false));

                // 팀 채팅 상태 유지, 단 팀이 변경된 경우 새 팀의 채팅으로 설정
                if (!oldTeam.equals(newTeam)) {
                    teamChatEnabled.put(player, false);  // 팀이 변경되면 팀 채팅을 기본적으로 비활성화
                    player.sendMessage(Prefix.SERVER + "당신의 팀이 " + oldTeam + "에서 " + newTeam + "으로 변경되었습니다. 팀 채팅이 비활성화되었습니다.");
                } else {
                    teamChatEnabled.put(player, playerTeamChatStates.getOrDefault(player, false));
                }

                // 새 팀의 스폰 위치로 텔레포트
                Location newSpawn = getRandomSpawnLocation(player);
                player.teleport(newSpawn);

                // 팀이 변경된 경우에만 메시지 전송
                if (!oldTeam.equals(newTeam)) {
                    player.sendMessage(Prefix.SERVER + "당신의 팀이 " + oldTeam + "에서 " + newTeam + "으로 변경되었습니다.");
                }
            }
        }

        // 스코어보드 재설정
        setupTeamsAndScoreboard();

        // 짧은 지연 후 모든 플레이어를 서바이벌 모드로 변경
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : allPlayers) {
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.getInventory().clear();  // 인벤토리 초기화
                giveGameItems(player);
                updatePlayerScoreboard(player);
            }
        }, 20L); // 1초 후

        broadcastToPlayers(Prefix.SERVER + "팀 밸런스를 위해 팀이 재조정되었습니다.");
    }

    private void updatePlayerScoreboard(Player player) {
        if (scoreboard != null && objective != null) {
            player.setScoreboard(scoreboard);
            objective.getScore(player.getName()).setScore(scores.getOrDefault(player, 0));
        }
    }
    protected void startGameTimer() {
        bossBar = Bukkit.createBossBar("남은 시간: " + gameTimeLimit + "초", BarColor.BLUE, BarStyle.SOLID);
        for (Player player : players) {
            bossBar.addPlayer(player);
        }

        new BukkitRunnable() {
            int timeLeft = gameTimeLimit;

            @Override
            public void run() {
                if (timeLeft <= 0 || state != MinigameState.IN_PROGRESS) {
                    if (bossBar != null) {
                        bossBar.removeAll();
                        bossBar = null;
                    }
                    this.cancel();
                    if (state == MinigameState.IN_PROGRESS) {
                        endGame(false);
                    }
                    return;
                }

                bossBar.setTitle("남은 시간: " + timeLeft + "초");
                bossBar.setProgress((double) timeLeft / gameTimeLimit);
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    protected void handlePlayerDeath(Player player) {
        Location deathLocation = player.getLocation();

        // 즉시 리스폰
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.spigot().respawn();
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(deathLocation);
            player.sendTitle("§c사망", "§e3초 후 리스폰됩니다", 10, 40, 10);

            // 3초 후 리스폰 처리
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(getRandomSpawnLocation(player));
                giveGameItems(player);
                player.setHealth(player.getMaxHealth());
            }, 60L); // 3초
        }, 1L); // 다음 틱에 실행
    }
    // 유틸리티 메서드
    protected void broadcastToPlayers(String message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

    protected void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : players) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    protected void broadcastToServer(String message) {
        plugin.getServer().broadcastMessage(message);
    }

    protected Location getRandomSpawnLocation(Player player) {
        List<Location> spawnLocations;
        if (teamType == TeamType.SOLO) {
            spawnLocations = config.getSoloLocations();
        } else {
            String team = getPlayerTeam(player);
            int teamNumber = Integer.parseInt(team.replaceAll("\\D+", ""));
            spawnLocations = config.getTeamSpawnLocations(teamType.name().toLowerCase(), teamNumber);
        }

        if (spawnLocations == null || spawnLocations.isEmpty()) {
            player.sendMessage(Prefix.SERVER + "§c스폰 위치가 설정되어 있지 않습니다. 관리자에게 문의하세요.");
            return config.getGameLobbyLocation();
        }
        return spawnLocations.get(new Random().nextInt(spawnLocations.size()));
    }

    private void teleportToGameLobby(Player player) {
        Location lobbyLocation = config.getGameLobbyLocation();
        if (lobbyLocation != null) {
            plugin.safelyTeleportPlayer(player, lobbyLocation);
        } else {
            player.sendMessage(Prefix.SERVER + "게임 로비 위치가 설정되지 않았습니다.");
        }
    }

    private void preparePlayerForGame(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(8, HELPER_ITEM);
    }

    private void initializePlayerForGame(Player player) {
        player.getInventory().clear();
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);
        player.setGameMode(GameMode.SURVIVAL);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setInvulnerable(false);
    }

    private void resetGame() {
        resetKDStats();
        allPlayerResetScoreboard();
        players.clear();
        scores.clear();
        playerTeams.clear();
        teams.clear();
        teamChatEnabled.clear();
        resetKDStats();
        disconnectedPlayers.clear();
        for (Player player : players) {
            MinigamePenaltyManager.clearPlayerData(player.getUniqueId());
        }

        if (!isTeamGame) {
            for (int i = 1; i <= MAX_PLAYER; i++) {
                teams.put("개인" + i, new ArrayList<>());
                teamType = TeamType.SOLO;
            }
        } else {
            for (int i = 1; i <= numberOfTeams; i++) {
                teams.put("팀" + i, new ArrayList<>()); // 팀 초기화
            }
        }

        scoreboardTeams.clear();
        current_player = 0;
        countdownTime = DEFAULT_COUNTDOWN_TIME; // 카운트다운 시간 초기화

    }

    private void allPlayerResetScoreboard() {
        for (Player player : players) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard()); // 스코어보드를 기본값으로 되돌림
        }
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        state = MinigameState.WAITING;
    }

    // 자발적인 명령어로 탈주했을 때
    public void handleCommandQuit(Player player) {
        if (state == MinigameState.IN_PROGRESS) {
            MinigamePenaltyManager.handlePlayerDisconnect(player, COMMAND_MAIN_NAME);
            disconnectedPlayers.add(player.getUniqueId());
        }
        gameQuitPlayer(player);
        broadcastToPlayers(Prefix.SERVER + player.getName() + "님이 " + getDisplayGameName() + " 게임에서 퇴장하셨습니다.");
    }

    public void handlePlayerQuit(Player player) {
        if (state == MinigameState.IN_PROGRESS) {
            MinigamePenaltyManager.handlePlayerDisconnect(player, COMMAND_MAIN_NAME);
            disconnectedPlayers.add(player.getUniqueId());
        }
        gameQuitPlayer(player);
        broadcastToPlayers(Prefix.SERVER + player.getName() + "님이 " + getDisplayGameName() + " 게임에서 퇴장하셨습니다.");
    }

    public void handlePlayerReconnect(Player player) {
        if (state == MinigameState.ENDING) {
            player.sendMessage(Prefix.SERVER + "게임이 종료 중이어서 재참여할 수 없습니다.");
            plugin.teleportToSpawn(player);
            return;
        }
        if (state == MinigameState.STARTING) {
            plugin.teleportToSpawn(player);
            return;
        }
        if (MinigamePenaltyManager.handlePlayerReconnect(player, COMMAND_MAIN_NAME)) {
            rejoinPlayer(player);
            player.sendMessage(Prefix.SERVER + "게임 균형을 위해 30초 동안 관전 모드로 설정됩니다.");
        } else {
            plugin.teleportToSpawn(player);
        }
    }

    public void rejoinPlayer(Player player) {
        UUID playerId = player.getUniqueId();

        if (players.contains(player)) {
            player.sendMessage(Prefix.SERVER + "이미 게임에 참여 중입니다.");
            return;
        }

        players.add(player);
        current_player++;

        String team = playerTeams.get(playerId);
        if (team != null && !teams.get(team).contains(player)) {
            teams.get(team).add(player);
        } else if (team == null) {
            team = findTeamToJoin();
            if (team != null) {
                assignPlayerToTeam(player, team);
            } else {
                player.sendMessage(Prefix.SERVER + "참여 가능한 팀이 없습니다.");
                players.remove(player);
                current_player--;
                return;
            }
        }

        // 이전 팀 채팅 상태 복원
        boolean wasTeamChatEnabled = teamChatEnabled.getOrDefault(player, false);
        teamChatEnabled.put(player, wasTeamChatEnabled);

        // 플레이어의 이전 점수와 킬/데스 복원
        scores.putIfAbsent(player, 0);
        kills.putIfAbsent(player, 0);
        deaths.putIfAbsent(player, 0);

        // 플레이어를 관전 모드로 설정
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(getRandomSpawnLocation(player));
        player.sendMessage(Prefix.SERVER + "게임에 다시 참여하셨습니다. 30초 동안 관전 모드입니다.");

        // 스코어보드에 플레이어 추가
        if (scoreboard != null && objective != null) {
            objective.getScore(player.getName()).setScore(scores.get(player));
        }

        // BossBar에 플레이어 추가
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }

        // 30초 후 플레이어 상태 복구
        Bukkit.getScheduler().runTaskLater(plugin, () -> restorePlayerAfterRejoin(player), REJOIN_COOLDOWN / 50);
    }

    private void restorePlayerAfterRejoin(Player player) {
        if (players.contains(player) && state == MinigameState.IN_PROGRESS) {
            player.setGameMode(GameMode.SURVIVAL);
            giveGameItems(player);
            player.teleport(getRandomSpawnLocation(player));
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.sendMessage(Prefix.SERVER + "관전 모드가 해제되었습니다. 게임에 참여할 수 있습니다.");
        }
    }

    protected void checkGameState() {
        if (getState() == MinigameState.IN_PROGRESS && getPlayers().size() < MIN_PLAYER) {
            broadcastToPlayers(Prefix.SERVER + "인원 부족으로 자동으로 게임이 종료됩니다.");
            endGame(true);
        } else if (getState() == MinigameState.STARTING && getPlayers().size() < MIN_PLAYER) {
            cancelCountdown();
            broadcastToPlayers(Prefix.SERVER + "인원 부족으로 게임 시작이 취소되었습니다.");
            countdownTime = DEFAULT_COUNTDOWN_TIME;
        }
    }

    // kda

    public void addKill(Player player) {
        kills.put(player, kills.getOrDefault(player, 0) + 1);
        if (isTeamGame) {
            String team = getPlayerTeam(player);
            teamKills.put(team, teamKills.getOrDefault(team, 0) + 1);
        }
        updateScoreboard();
    }

    public void addDeath(Player player) {
        deaths.put(player, deaths.getOrDefault(player, 0) + 1);
        if (isTeamGame) {
            String team = getPlayerTeam(player);
            teamDeaths.put(team, teamDeaths.getOrDefault(team, 0) + 1);
        }
        updateScoreboard();
    }

    public double getKDRatio(Player player) {
        int playerKills = kills.getOrDefault(player, 0);
        int playerDeaths = deaths.getOrDefault(player, 1); // 0으로 나누는 것을 방지
        return (double) playerKills / playerDeaths;
    }

    public double getTeamKDRatio(String team) {
        int teamKillCount = teamKills.getOrDefault(team, 0);
        int teamDeathCount = teamDeaths.getOrDefault(team, 1); // 0으로 나누는 것을 방지
        return (double) teamKillCount / teamDeathCount;
    }

    public void showKDRatio(Player player) {
        double kdRatio = getKDRatio(player);
        player.sendMessage(Prefix.SERVER + "당신의 K/D 비율: " + String.format("%.2f", kdRatio));
        if (isTeamGame) {
            String team = getPlayerTeam(player);
            double teamKDRatio = getTeamKDRatio(team);
            player.sendMessage(Prefix.SERVER + "팀의 K/D 비율: " + String.format("%.2f", teamKDRatio));
        }
    }

    private void resetKDStats() {
        kills.clear();
        deaths.clear();
        teamKills.clear();
        teamDeaths.clear();
    }
    // 아이템 지급
    protected abstract void giveGameItems(Player player);
    // 기본 아이템 지급 (모든 게임에 공통적으로 필요한 아이템이 있다면)

    protected void giveDefaultItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(8, HELPER_ITEM);
        if (isTeamGame) {
            String team = getPlayerTeam(player);
            giveColoredArmor(player, team);
        }
    }

    // 상태 변경
    public void setState(MinigameState state) {
        this.state = state;
    }


    public abstract void showRules(Player player);
    // 추상 메서드

    protected abstract void onGameStart();
    protected abstract void onGameEnd();

    protected abstract void setupGameSpecificRules();
    protected abstract void removePlayerFromScoreboard(Player player);

    protected abstract void updateScoreboard();
    // Getter 메서드
    public MinigameConfig getConfig() { return config; }
    public int getMIN_PLAYER() { return MIN_PLAYER; }
    public int getMAX_PLAYER() { return MAX_PLAYER; }
    public String getDisplayGameName() { return DISPLAY_GAME_NAME; }
    public String getCOMMAND_MAIN_NAME() { return COMMAND_MAIN_NAME; }
    public Set<Player> getPlayers() { return players; }
    public MinigameState getState() { return state; }
    public BossBar getBossBar() { return bossBar; }
    public BossBar getLobbyBossBar() { return lobbyBossBar; }
    public int getGameTimeLimit() { return gameTimeLimit; }



    public boolean isBlockBreakAllowed() { return blockBreakAllowed; }
    public boolean isBlockPlaceAllowed() { return blockPlaceAllowed; }
    public boolean isItemDropAllowed() { return itemDropAllowed; }
    public boolean isItemPickupAllowed() { return itemPickupAllowed; }
    public boolean isItemMoveAllowed() { return itemMoveAllowed; }
    public boolean isTeamGame() { return isTeamGame; }

    // 기타 메서드
    public void setPlayerListLocation(Player player, String location) {
        plugin.getPlayerList().put(player.getUniqueId(), location);
    }

    public void setGameTimeLimit(int seconds) {
        this.gameTimeLimit = seconds;
    }
}