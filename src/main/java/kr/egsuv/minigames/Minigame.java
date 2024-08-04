package kr.egsuv.minigames;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.config.MinigameConfig;
import kr.egsuv.data.MinigameData;
import kr.egsuv.data.PlayerData;
import kr.egsuv.util.ItemUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

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

    public Minigame(EGServerMain plugin, String commandMainName, int MIN_PLAYER, int MAX_PLAYER, String displayGameName, boolean isTeamGame) {
        this.plugin = plugin;
        this.COMMAND_MAIN_NAME = commandMainName;
        this.MIN_PLAYER = MIN_PLAYER;
        this.MAX_PLAYER = MAX_PLAYER;
        this.DISPLAY_GAME_NAME = displayGameName;
        this.isTeamGame = isTeamGame;
        this.config = new MinigameConfig(plugin, commandMainName);

        this.HELPER_ITEM = ItemUtils.createItem(Material.ENCHANTED_BOOK, 1, "§7[ §f게임 §a튜토리얼 §f읽기 §7]",
                "§7§l| §f우클릭 시 게임 하는 방법을 알아볼 수 있습니다.");
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
    }

    // 팀 게임 시작 시 플레이어들을 팀에 할당하는 메소드
    protected void assignTeams() {
        if (!isTeamGame) {
            return;
        }

        List<Player> playerList = new ArrayList<>(players);
        Collections.shuffle(playerList);

        for (int i = 0; i < playerList.size(); i++) {
            Player player = playerList.get(i);
            if (i % 2 == 0) {
                assignPlayerToTeam(player, "red");
            } else {
                assignPlayerToTeam(player, "blue");
            }
        }
    }
    // 게임 입장
    public void joinGame(Player player) {
        if (state != MinigameState.WAITING && state != MinigameState.STARTING) {
            player.sendMessage(Prefix.SERVER + DISPLAY_GAME_NAME + " 게임이 이미 진행 중입니다.");
            return;
        }

        if (players.contains(player)) {
            player.sendMessage(Prefix.SERVER + "이미 게임에 참여 중입니다.");
            return;
        }

        if (current_player >= MAX_PLAYER) {
            player.sendMessage(Prefix.SERVER + "현재 " + DISPLAY_GAME_NAME + " 게임이 꽉 찼습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        current_player++;
        players.add(player);
        updateLobbyBossBar();

        broadcastToPlayers(Prefix.SERVER + player.getName() + "님이 " + DISPLAY_GAME_NAME + " 게임에 입장하셨습니다. (" +
                current_player + "/" + MIN_PLAYER + ")");

        teleportToLobby(player);
        preparePlayerForGame(player);

        if (current_player >= MIN_PLAYER && state == MinigameState.WAITING) {
            startCountdown();
        }
    }

    protected void startCountdown() {
        if (state == MinigameState.STARTING) {
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
                if (countdownTime > 0) {
                    // broadcastToServer 조건
                    if (countdownTime == countdownTime / 2 || countdownTime == 10 || countdownTime == 60) {
                        broadcastToServer(Prefix.SERVER.toString() + countdownTime + "초 후 " + DISPLAY_GAME_NAME + " 게임이 시작됩니다.");
                    }

                    // broadcastToPlayers 조건
                    if (countdownTime <= 9) {
                        broadcastToPlayers(Prefix.SERVER.toString() + countdownTime + "초 후 " + DISPLAY_GAME_NAME + " 게임이 시작됩니다.");
                    }
                    countdownTime--;
                } else {
                    this.cancel();
                    startGame();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void startGame() {
        state = MinigameState.IN_PROGRESS;
        if (lobbyBossBar != null) {
            lobbyBossBar.removeAll();
            lobbyBossBar = null;
        }
        if (isTeamGame) {
            assignTeams();
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

        broadcastTitle("§6" + DISPLAY_GAME_NAME, "§e게임 시작!", 10, 70, 20);
        setupGameSpecificRules();
        onGameStart();
        startGameTimer();
    }

    public void endGame() {
        cancelCountdown();
        state = MinigameState.ENDING;

        // 모든 플레이어를 무적 상태로 만듦
        for (Player player : players) {
            player.setInvulnerable(true);
            player.sendMessage(Prefix.SERVER + " §c15초 §f후 게임이 종료됩니다.");
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
            for (Player player : playersToTeleport) {
                plugin.teleportToSpawn(player);
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                player.setInvulnerable(false);
            }

            broadcastToServer(Prefix.SERVER + DISPLAY_GAME_NAME + " 게임이 종료되었습니다!");

            resetGame();
        }, 300L); // 15초 (20틱 * 15 = 300틱)
    }

    protected void onGameEnd() {
        showFinalRanking();
        for (Player player : getPlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        broadcastToServer(Prefix.SERVER.toString() + ChatColor.YELLOW + getDisplayGameName() + " 게임이 종료되었습니다!");
    }

    // 강제 실행
    public void forceStart() {
        if (state == MinigameState.WAITING || state == MinigameState.STARTING) {
            if (players.size() >= 2) {
                if (countdownTask != null) {
                    countdownTask.cancel();
                }
                startGame();
            } else {
                broadcastToPlayers(Prefix.SERVER + "게임을 시작하기 위해서는 최소 2명의 플레이어가 필요합니다.");
            }
        } else {
            broadcastToPlayers(Prefix.SERVER + "게임이 이미 진행 중입니다.");
        }
    }

    // 로비 BossBar 업데이트
    protected void updateLobbyBossBar() {
        if (state == MinigameState.WAITING || state == MinigameState.STARTING) {
            if (lobbyBossBar == null) {
                lobbyBossBar = Bukkit.createBossBar("대기 중: " + current_player + "/" + MIN_PLAYER, BarColor.BLUE, BarStyle.SOLID);
            }
            lobbyBossBar.setTitle("대기 중: " + current_player + "/" + MIN_PLAYER);
            lobbyBossBar.setProgress((double) current_player / MIN_PLAYER);
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

        StringBuilder ranking = new StringBuilder("§6=== " + getDisplayGameName() + " 최종 랭킹 ===\n");
        for (int i = 0; i < Math.min(10, sortedScores.size()); i++) {
            Map.Entry<Player, Integer> entry = sortedScores.get(i);
            ranking.append("§e").append(i + 1).append(". §f").append(entry.getKey().getName()).append(": §b").append(entry.getValue()).append("\n");
        }

        for (Player player : getPlayers()) {
            player.sendMessage(ranking.toString());
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

            if (bossBar != null) {
                bossBar.removePlayer(player); // BossBar에서 플레이어 제거
            }

            if (lobbyBossBar != null) {
                lobbyBossBar.removePlayer(player);
            }
            checkGameState();
            updateLobbyBossBar();
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
                        bossBar = null; // BossBar 제거 후 null로 설정
                    }
                    this.cancel();
                    if (state == MinigameState.IN_PROGRESS) {
                        endGame();
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

            // 5초 후 리스폰 처리
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(getRandomSpawnLocation(player));
                giveGameItems(player);
                player.setHealth(player.getMaxHealth());
            }, 60L); // 5초
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
        if (isTeamGame) {
            // 팀 게임일 경우
            String team = getPlayerTeam(player); // 플레이어의 팀을 가져오는 메서드 (구현 필요)
            List<Location> teamLocations;
            if ("red".equals(team)) {
                teamLocations = config.getRedTeamLocations();
            } else {
                teamLocations = config.getBlueTeamLocations();
            }

            if (teamLocations.isEmpty()) {
                player.sendMessage(Prefix.SERVER + "§c팀 스폰 위치가 설정되어 있지 않습니다. 관리자에게 문의하세요.");
                return config.getGameLobbyLocation();
            }
            return teamLocations.get(new Random().nextInt(teamLocations.size()));
        } else {
            // 개인전일 경우
            List<Location> locations = config.getSoloLocations();
            if (locations.isEmpty()) {
                player.sendMessage(Prefix.SERVER + "§c스폰 위치가 설정되어 있지 않습니다. 관리자에게 문의하세요.");
                return config.getGameLobbyLocation();
            }
            return locations.get(new Random().nextInt(locations.size()));
        }
    }

    private void teleportToLobby(Player player) {
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
        allPlayerResetScoreboard();
        players.clear();
        scores.clear();
        playerTeams.clear();
        current_player = 0;
        state = MinigameState.WAITING;

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

    public void handlePlayerQuit(Player player) {
        gameQuitPlayer(player);
        broadcastToPlayers(Prefix.SERVER + player.getName() + "님이 게임에서 퇴장하셨습니다.");
        removePlayerFromScoreboard(player);
    }

    protected void checkGameState() {
        if (getState() == MinigameState.IN_PROGRESS && getPlayers().size() < MIN_PLAYER) {
            broadcastToPlayers(Prefix.SERVER + "인원 부족으로 자동으로 게임이 종료됩니다.");
            endGame();
        } else if (getState() == MinigameState.STARTING && getPlayers().size() < MIN_PLAYER) {
            cancelCountdown();
            broadcastToPlayers(Prefix.SERVER + "인원 부족으로 게임 시작이 취소되었습니다.");
            countdownTime = DEFAULT_COUNTDOWN_TIME;
        }
    }

    public abstract void showRules(Player player);

    // 추상 메서드
    protected abstract void onGameStart();

    protected abstract void setupScoreboard();
    protected abstract void giveGameItems(Player player);
    protected abstract void setupGameSpecificRules();
    protected abstract void removePlayerFromScoreboard(Player player);

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