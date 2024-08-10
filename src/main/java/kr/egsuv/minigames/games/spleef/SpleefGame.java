package kr.egsuv.minigames.games.spleef;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameItems;
import kr.egsuv.minigames.MinigameState;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class SpleefGame extends Minigame implements Listener {

    private static final int ROUNDS_TO_WIN = 3;
    private final Material SPLEEF_TOOL = Material.DIAMOND_SHOVEL;
    private static final int POWERUP_SPAWN_INTERVAL = 30; // 20초마다 파워업 생성
    private static final int INITIAL_SUDDEN_DEATH_TIME = 50; // 초기 서든 데스 시작 시간 (50초)
    private static final int INITIAL_FIREBALL_INTERVAL = 5; // 초기 화염구 생성 간격 (5초)
    private static final double SNOW_DROP_CHANCE = 0.05; // 5% 확률로 눈덩이 획득
    private static final int FLASH_JUMP_COOLDOWN = 8; // 플래시 점프 쿨타임 (초)

    private List<Player> alivePlayers;
    private Map<Player, Integer> roundWins;
    private int currentRound = 0;
    private BukkitTask gameTask;
    private BukkitTask powerupTask;
    private BukkitTask suddenDeathTask;
    private BukkitTask fireballTask;
    private Set<Location> brokenBlocks = new HashSet<>();
    private Location arenaCenter;
    private int arenaRadius;
    private int lowestY;
    private Location arenaMin;
    private Location arenaMax;
    private Map<Player, Long> lastFlashJumpTime = new HashMap<>();

    public SpleefGame(EGServerMain plugin, MinigameItems item, String commandMainName, int MIN_PLAYER, int MAX_PLAYER, String displayGameName, boolean useBlockRestore) {
        super(plugin, item, commandMainName, MIN_PLAYER, MAX_PLAYER, displayGameName, useBlockRestore);
        setGameRules(true, false, false, true, false);
        setGameTimeLimit(300); // 5분 라운드 제한시간
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    protected void onGameStart() {
        alivePlayers = new ArrayList<>(getPlayers());
        roundWins = new HashMap<>();
        for (Player player : getPlayers()) {
            roundWins.put(player, 0);
            scores.put(player, 0);
            // 플레쉬 점프 기록
            lastFlashJumpTime.put(player, 0L);
        }
        setArenaProperties();
        startNextRound();
        updateScoreboard();
    }

    @Override
    protected boolean loadGameData() {
        return false;
    }

    @Override
    protected void onGameEnd() {

    }

    private void setArenaProperties() {
        arenaCenter = getArenaCenter();
        if (arenaCenter == null) {
            plugin.getLogger().warning(currentMap + " 맵의 중심 좌표를 찾을 수 없습니다.");
            return;
        }
        calculateArenaBounds(); // 이 부분을 추가합니다.
        lowestY = findLowestSnowBlock();
    }

    private void calculateArenaBounds() {
        World world = arenaCenter.getWorld();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        int searchRadius = 100; // 검색 반경 설정
        int searchHeight = 50;  // 수직 검색 범위 설정

        for (int x = arenaCenter.getBlockX() - searchRadius; x <= arenaCenter.getBlockX() + searchRadius; x++) {
            for (int y = arenaCenter.getBlockY() - searchHeight; y <= arenaCenter.getBlockY() + searchHeight; y++) {
                for (int z = arenaCenter.getBlockZ() - searchRadius; z <= arenaCenter.getBlockZ() + searchRadius; z++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.SNOW_BLOCK) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        maxZ = Math.max(maxZ, z);
                    }
                }
            }
        }

        arenaMin = new Location(world, minX, minY, minZ);
        arenaMax = new Location(world, maxX, maxY, maxZ);
        arenaRadius = Math.max((maxX - minX) / 2, (maxZ - minZ) / 2);

        plugin.getLogger().info("아레나 범위 설정 완료: " +
                "Min(" + minX + ", " + minY + ", " + minZ + ") " +
                "Max(" + maxX + ", " + maxY + ", " + maxZ + ") " +
                "Radius: " + arenaRadius);
    }

    private int findLowestSnowBlock() {
        World world = arenaCenter.getWorld();
        for (int y = arenaMin.getBlockY(); y <= arenaMax.getBlockY(); y++) {
            for (int x = arenaMin.getBlockX(); x <= arenaMax.getBlockX(); x++) {
                for (int z = arenaMin.getBlockZ(); z <= arenaMax.getBlockZ(); z++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.SNOW_BLOCK) {
                        return y; // 가장 낮은 눈 블록의 Y 좌표를 반환합니다.
                    }
                }
            }
        }
        return arenaMin.getBlockY(); // 눈 블록을 찾지 못한 경우 기본값 반환
    }
    @Override
    protected void resumeGame() {
        currentRound++;
        alivePlayers = new ArrayList<>(getPlayers());
        brokenBlocks.clear();

        for (Player player : getPlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(getRandomSpawnLocation(player));
            player.getInventory().clear();
            giveGameItems(player);
            player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            player.setFoodLevel(20);
        }

        broadcastTitle("§6라운드 " + currentRound, "§e눈을 파서 상대를 떨어뜨리세요!", 10, 70, 20);

        startGameTasks();
    }

    private void startGameTasks() {
        cancelGameTasks(); // 이전 Task들을 취소

        gameTask = runTaskTimer(() -> {
            checkPlayersBelow();
            if (alivePlayers.size() <= 1) {
                endRound();
            }
        }, 0L, 10L);

        powerupTask = runTaskTimer(this::spawnPowerup, POWERUP_SPAWN_INTERVAL * 20L, POWERUP_SPAWN_INTERVAL * 20L);

        int suddenDeathTime = Math.max(10, INITIAL_SUDDEN_DEATH_TIME - (currentRound - 1) * 5);
        suddenDeathTask = runTaskLater(this::startSuddenDeath, suddenDeathTime * 20L);

        // 게임 종료 30초 전 이벤트 예약
        runTaskLater(this::scheduleSuddenBlockRemoval, (getGameTimeLimit() - 30) * 20L);
    }

    private void checkPlayersBelow() {
        for (Player player : new ArrayList<>(alivePlayers)) {
            if (player.getLocation().getY() < (lowestY - 5)) { // 5블록 정도의 여유를 둡니다.
                eliminatePlayer(player);
            }
        }
    }

    private void eliminatePlayer(Player player) {
        alivePlayers.remove(player);
        player.setGameMode(GameMode.SPECTATOR);
        broadcastToPlayers(Component.text("§c" + player.getName() + "§6님이 탈락했습니다!"));
        player.sendTitle("§c탈락!", "§e다음 라운드를 기다려주세요.", 10, 70, 20);

        int rank = alivePlayers.size() + 1;
        int scoreToAdd = getPlayers().size() - rank + 1;
        scores.put(player, scores.getOrDefault(player, 0) + scoreToAdd);

        updateScoreboard();
    }

    private void endRound() {
        cancelGameTasks();
        updateScoreboard();

        Player roundWinner = alivePlayers.isEmpty() ? null : alivePlayers.get(0);
        if (roundWinner != null) {
            int wins = roundWins.getOrDefault(roundWinner, 0) + 1;
            roundWins.put(roundWinner, wins);
            scores.put(roundWinner, scores.getOrDefault(roundWinner, 0) + getPlayers().size());
            broadcastToPlayers(Component.text("§6" + roundWinner.getName() + "§e님이 라운드 " + currentRound + "에서 승리했습니다!"));

            if (wins >= ROUNDS_TO_WIN) {
                updateScoreboard(); // 마지막 라운드 스코어 업데이트
                endGame(false);
                return;
            }
        } else {
            broadcastToPlayers(Component.text("§6라운드 " + currentRound + "§e가 무승부로 종료되었습니다."));
        }

        updateScoreboard();
        checkGameEnd();
        alivePlayers = new ArrayList<>(getPlayers());

        if (useBlockRestore) {
            gameRestoration();
        } else {
            startNextRound();
        }
    }

    @Override
    public void handlePlayerQuit(Player player) {
        super.handlePlayerQuit(player);
        if (alivePlayers != null) {
            alivePlayers.remove(player);
        }
        if (lastFlashJumpTime != null) {
            lastFlashJumpTime.remove(player);
        }
        if (roundWins != null) {
            roundWins.remove(player);
        }
        if (alivePlayers.size() <= 1) {
            endRound();
        }
    }


    private void checkGameEnd() {
        boolean gameEnded = false;
        for (Map.Entry<Player, Integer> entry : roundWins.entrySet()) {
            if (entry.getValue() >= ROUNDS_TO_WIN) {
                gameEnded = true;
                break;
            }
        }

        if (gameEnded || currentRound >= ROUNDS_TO_WIN * 2 - 1) {
            endGame(false);
        }
    }

    private void cancelGameTasks() {
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        if (powerupTask != null) {
            powerupTask.cancel();
            powerupTask = null;
        }
        if (suddenDeathTask != null) {
            suddenDeathTask.cancel();
            suddenDeathTask = null;
        }
        if (fireballTask != null) {
            fireballTask.cancel();
            fireballTask = null;
        }
    }


    private void spawnPowerup() {
        if (brokenBlocks.isEmpty()) {
            return;
        }
        Location spawnLoc = new ArrayList<>(brokenBlocks).get(new Random().nextInt(brokenBlocks.size()));
        Item powerupItem = spawnLoc.getWorld().dropItem(spawnLoc.clone().add(0.5, 0.5, 0.5), createPowerupItem());
        powerupItem.setGlowing(true);
        broadcastToPlayers(Component.text("§a파워업이 생성되었습니다!"));
    }

    private ItemStack createPowerupItem() {
        ItemStack powerup = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = powerup.getItemMeta();
        meta.setDisplayName("§b§l파워업");
        powerup.setItemMeta(meta);
        return powerup;
    }

    private void startSuddenDeath() {
        broadcastTitle("§c서든 데스", "§e화염구가 내려옵니다!", 10, 70, 20);
        int fireballInterval = Math.max(1, INITIAL_FIREBALL_INTERVAL - (currentRound - 1));
        fireballTask = runTaskTimer(this::spawnFireball, 0L, fireballInterval * 20L);
    }

    private void spawnFireball() {
        if (alivePlayers.size() <= 1 || getState() != MinigameState.IN_PROGRESS) {
            return;
        }

        int fireballCount = Math.min(5, currentRound);
        for (int i = 0; i < fireballCount; i++) {
            Location spawnLoc = new Location(arenaCenter.getWorld(),
                    arenaMin.getX() + Math.random() * (arenaMax.getX() - arenaMin.getX()),
                    arenaMax.getY() + 5,
                    arenaMin.getZ() + Math.random() * (arenaMax.getZ() - arenaMin.getZ())
            );

            Fireball fireball = (Fireball) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.FIREBALL);
            fireball.setDirection(new Vector(0, -1, 0).normalize());
            fireball.setYield(2F);
            fireball.setIsIncendiary(false);
        }
    }

    private void removeRandomFloorBlock() {
        List<Location> floorBlocks = getFloorBlocks();
        if (!floorBlocks.isEmpty()) {
            Location blockToRemove = floorBlocks.get(new Random().nextInt(floorBlocks.size()));
            blockToRemove.getBlock().setType(Material.AIR);
            brokenBlocks.add(blockToRemove);
        }
    }

    private List<Location> getFloorBlocks() {
        List<Location> floorBlocks = new ArrayList<>();
        World world = arenaCenter.getWorld();

        for (int y = arenaMin.getBlockY(); y <= arenaMax.getBlockY(); y++) {
            for (int x = arenaMin.getBlockX(); x <= arenaMax.getBlockX(); x++) {
                for (int z = arenaMin.getBlockZ(); z <= arenaMax.getBlockZ(); z++) {
                    Location loc = new Location(world, x, y, z);
                    if (loc.getBlock().getType() == Material.SNOW_BLOCK) {
                        floorBlocks.add(loc);
                    }
                }
            }
        }
        return floorBlocks;
    }

    @Override
    protected void giveGameItems(Player player) {
        player.getInventory().clear();
        ItemStack spleefTool = new ItemStack(SPLEEF_TOOL);
        spleefTool.addUnsafeEnchantment(Enchantment.EFFICIENCY, 5);
        ItemMeta meta = spleefTool.getItemMeta();
        meta.setDisplayName("§b§l스플리프 삽");
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add("§e우클릭: 플래시 점프");
        meta.setLore(lore);
        spleefTool.setItemMeta(meta);
        player.getInventory().setItem(0, spleefTool);
    }


    @Override
    protected void updateScoreboard() {
        if (scoreboard == null || objective == null) return;

        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        objective.setDisplayName("§6§lSpleef - 라운드 " + currentRound);

        List<Map.Entry<Player, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        int scoreValue = 15;
        for (Map.Entry<Player, Integer> entry : sortedScores) {
            String playerName = entry.getKey().getName();
            int score = entry.getValue();
            int wins = roundWins.getOrDefault(entry.getKey(), 0);
            String displayText = String.format("§a%s: §f%d점 (%d승)", playerName, score, wins);
            objective.getScore(displayText).setScore(scoreValue--);

            if (scoreValue <= 0) break; // 최대 15명까지만 표시
        }

        for (Player player : getPlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    // 갑자기 모든 블록을 제거하는 이벤트 (게임 종료 30초 전)
    private void scheduleSuddenBlockRemoval() {
        if (getState() == MinigameState.IN_PROGRESS) {
            broadcastTitle("§c§l경고!", "§e모든 블록이 10초 후 사라집니다!", 10, 70, 20);
            runTaskLater(this::removeAllBlocks, 200L);
        }
    }

    private void removeAllBlocks() {
        World world = arenaCenter.getWorld();
        int minY = lowestY;
        int maxY = arenaCenter.getBlockY() + 1;

        for (int y = minY; y <= maxY; y++) {
            for (int x = arenaCenter.getBlockX() - arenaRadius; x <= arenaCenter.getBlockX() + arenaRadius; x++) {
                for (int z = arenaCenter.getBlockZ() - arenaRadius; z <= arenaCenter.getBlockZ() + arenaRadius; z++) {
                    Location loc = new Location(world, x, y, z);
                    if (loc.getBlock().getType() == Material.SNOW_BLOCK) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        broadcastToPlayers(Component.text("§c§l모든 블록이 제거되었습니다! 조심하세요!"));
    }


    @Override
    public void showRules(Player player) {
        player.sendMessage("§6=== Spleef 게임 규칙 ===");
        player.sendMessage("§e1. 다이아몬드 삽으로 상대방 발 밑의 눈 블록을 부수세요.");
        player.sendMessage("§e2. 마지막까지 떨어지지 않고 생존하면 승리합니다.");
        player.sendMessage("§e3. 총 " + ROUNDS_TO_WIN + "라운드를 먼저 이기는 플레이어가 최종 승자입니다.");
        player.sendMessage("§e4. 각 라운드는 " + (getGameTimeLimit() / 60) + "분간 진행됩니다.");
        player.sendMessage("§e5. 30초마다 파워업이 생성됩니다. 파워업을 주워 특별한 능력을 얻으세요!");
        player.sendMessage("§e6. 1분 후 서든 데스가 시작되며, 화염구가 떨어집니다.");
        player.sendMessage("§e7. 삽을 우클릭하면 플래시 점프를 사용할 수 있습니다. (쿨타임: " + FLASH_JUMP_COOLDOWN + "초)");
        player.sendMessage("§e8. 눈 블록을 부술 때 일정 확률로 눈덩이를 얻을 수 있습니다.");
    }

    @Override
    protected void resetGameSpecificData() {
        currentRound = 0;
        roundWins.clear();
        brokenBlocks.clear();
        lastFlashJumpTime.clear();
        cancelGameTasks();
        alivePlayers.clear();
    }

    @Override
    protected void setupGameSpecificRules() {
        // 스플리프 게임에 특별한 규칙 설정이 필요 없음
    }

    @Override
    protected void removePlayerFromScoreboard(Player player) {
        if (scoreboard != null && objective != null) {
            for (String entry : scoreboard.getEntries()) {
                if (entry.contains(player.getName())) {
                    scoreboard.resetScores(entry);
                }
            }
        }
    }

    @Override
    public void applyCustomKillStreakBonus(Player player, int streak) {
        // 스플리프 게임에서는 킬 스트릭 보너스를 적용하지 않음
    }

    @Override
    public void removeCustomKillStreakEffects(Player player) {
        // 스플리프 게임에서는 킬 스트릭 효과를 제거할 필요 없음
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!getPlayers().contains(player) || getState() != MinigameState.IN_PROGRESS) {
            return;
        }

        if (isPlayerInGameLobby(player)) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlock();
        if (!isWithinArena(block.getLocation()) || block.getType() != Material.SNOW_BLOCK) {
            event.setCancelled(true);
            return;
        }


        if (player.getInventory().getItemInMainHand().getType() != SPLEEF_TOOL) {
            event.setCancelled(true);
            player.sendMessage("§c스플리프 삽으로만 눈 블록을 부술 수 있습니다!");
            return;
        }

        event.setDropItems(false);
        brokenBlocks.add(block.getLocation());
        block.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, block.getLocation().add(0.5, 0.5, 0.5), 10);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_SNOW_BREAK, 1.0f, 1.0f);

        if (Math.random() < SNOW_DROP_CHANCE) {
            player.getInventory().addItem(new ItemStack(Material.SNOWBALL));
            player.sendMessage("§a눈덩이를 획득했습니다!");
        }

        if (Math.random() < 0.1) {
            spawnPowerup();
        }
    }

    private boolean isWithinArena(Location location) {
        return location.getX() >= arenaMin.getX() && location.getX() <= arenaMax.getX() &&
                location.getY() >= arenaMin.getY() && location.getY() <= arenaMax.getY() &&
                location.getZ() >= arenaMin.getZ() && location.getZ() <= arenaMax.getZ();
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Fireball && getState() == MinigameState.IN_PROGRESS) {
            List<Block> blocksToRemove = new ArrayList<>();
            for (Block block : event.blockList()) {
                if (isWithinArena(block.getLocation())) {
                    if (block.getType() == Material.SNOW_BLOCK) {
                        brokenBlocks.add(block.getLocation());
                        block.setType(Material.AIR);
                    } else {
                        blocksToRemove.add(block);
                    }
                } else {
                    blocksToRemove.add(block);
                }
            }
            event.blockList().removeAll(blocksToRemove);
            event.setYield(0); // 폭발 효과 제거
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!getPlayers().contains(player) || getState() != MinigameState.IN_PROGRESS) {
            return;
        }

        if (isPlayerInGameLobby(player)) {
            event.setCancelled(true);
            return;
        }


        if (alivePlayers == null) return;

        if (player.getLocation().getY() < (lowestY - 5) && alivePlayers.contains(player)) {
            eliminatePlayer(player);
        }

        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 0.1, 0), 1, 0, 0, 0, 0);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (!getPlayers().contains(player) || getState() != MinigameState.IN_PROGRESS) {
            return;
        }

        event.setCancelled(true);
    }



    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!getPlayers().contains(player) || getState() != MinigameState.IN_PROGRESS) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item.getType() == SPLEEF_TOOL) {
                handleFlashJump(player);
            }
        }
    }

    private void handleFlashJump(Player player) {
        long currentTime = System.currentTimeMillis();
        long lastUseTime = lastFlashJumpTime.getOrDefault(player, 0L);

        if (currentTime - lastUseTime < FLASH_JUMP_COOLDOWN * 1000) {
            long remainingCooldown = FLASH_JUMP_COOLDOWN - (currentTime - lastUseTime) / 1000;
            player.sendMessage("§c플래시 점프 쿨타임: " + remainingCooldown + "초");
            return;
        }

        Vector direction = player.getLocation().getDirection().multiply(2.0);
        player.setVelocity(direction);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        player.spawnParticle(Particle.END_ROD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

        lastFlashJumpTime.put(player, currentTime);

        // 쿨타임 UI 표시
        new BukkitRunnable() {
            int secondsLeft = FLASH_JUMP_COOLDOWN;

            @Override
            public void run() {
                if (secondsLeft > 0) {
                    double progress = (double) secondsLeft / FLASH_JUMP_COOLDOWN;
                    String barColor = progress > 0.5 ? "§c" : (progress > 0.25 ? "§e" : "§a");
                    String progressBar = StringUtils.repeat("█", (int) (progress * 10)) +
                            StringUtils.repeat("░", 10 - (int) (progress * 10));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(barColor + "플래시 점프 쿨타임: " + progressBar + " " + secondsLeft + "초"));
                    secondsLeft--;
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText("§a플래시 점프 사용 가능!"));
                    player.sendTitle("", "§a플래시 점프 사용 가능!", 10, 40, 10);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSnowballThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;


        Player shooter = (Player) event.getEntity().getShooter();
        if (!getPlayers().contains(shooter) || getState() != MinigameState.IN_PROGRESS) {
            return;
        }

        Snowball snowball = (Snowball) event.getEntity();
        snowball.setVelocity(snowball.getVelocity().multiply(1.5)); // 눈덩이 속도 증가
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSnowballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player shooter = (Player) event.getEntity().getShooter();
        if (!getPlayers().contains(shooter) || getState() != MinigameState.IN_PROGRESS) {
            return;
        }

        if (event.getHitBlock() != null && event.getHitBlock().getType() == Material.SNOW_BLOCK) {
            event.getHitBlock().setType(Material.AIR);
            brokenBlocks.add(event.getHitBlock().getLocation());
            event.getHitBlock().getWorld().spawnParticle(Particle.ITEM_SNOWBALL, event.getHitBlock().getLocation().add(0.5, 0.5, 0.5), 20);
            event.getHitBlock().getWorld().playSound(event.getHitBlock().getLocation(), Sound.BLOCK_SNOW_BREAK, 1.0f, 1.0f);
        }
    }


    // 파워업 아이템 획득 처리
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (!getPlayers().contains(player) || getState() != MinigameState.IN_PROGRESS) {
            return;
        }

        if (isPlayerInGameLobby(player)) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.NETHER_STAR) {
            event.setCancelled(true);
            event.getItem().remove();
            applyPowerup(player);
        }
    }

    private void applyPowerup(Player player) {
        int randomEffect = new Random().nextInt(4);
        switch (randomEffect) {
            case 0:
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
                player.sendMessage("§a이동 속도가 10초간 증가합니다!");
                break;
            case 1:
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, 1));
                player.sendMessage("§a점프력이 10초간 증가합니다!");
                break;
            case 2:
                ItemStack superShovel = new ItemStack(SPLEEF_TOOL);
                superShovel.addUnsafeEnchantment(Enchantment.EFFICIENCY, 10);
                ItemMeta meta = superShovel.getItemMeta();
                meta.setDisplayName("§b§l초강력 스플리프 삽");
                meta.setUnbreakable(true);
                superShovel.setItemMeta(meta);
                player.getInventory().setItem(0, superShovel);
                player.sendMessage("§a10초간 초강력 삽을 사용할 수 있습니다!");
                Bukkit.getScheduler().runTaskLater(plugin, () -> giveGameItems(player), 200L);
                break;
            case 3:
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                player.sendMessage("§a5초간 체력이 빠르게 회복됩니다!");
                break;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
    }

    // 플레이어 채팅 이벤트 처리 (팀 채팅 구현)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!getPlayers().contains(player) || getState() != MinigameState.IN_PROGRESS) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();
        if (message.startsWith("!")) {
            // 전체 채팅
            broadcastToPlayers(Component.text("§7[전체] §f" + player.getName() + ": " + message.substring(1)));
        } else {
            // 팀 채팅 (관전자끼리)
            if (!alivePlayers.contains(player)) {
                for (Player p : getPlayers()) {
                    if (!alivePlayers.contains(p)) {
                        p.sendMessage("§8[관전] §7" + player.getName() + ": " + message);
                    }
                }
            } else {
                // 생존자는 전체 채팅만 가능
                player.sendMessage("§c생존 중에는 전체 채팅만 가능합니다. 메시지 앞에 !를 붙여주세요.");
            }
        }
    }
}