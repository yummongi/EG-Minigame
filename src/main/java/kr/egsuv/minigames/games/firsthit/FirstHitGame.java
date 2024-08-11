package kr.egsuv.minigames.games.firsthit;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameItems;
import kr.egsuv.minigames.MinigameState;
import kr.egsuv.util.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;
import java.util.*;

public class FirstHitGame extends Minigame implements Listener {

    private static final int WIN_SCORE = 50;
    private static final int DEBUFF_SCORE_THRESHOLD = 70; // 디버프를 받는 점수 기준
    private static final int BONUS_EFFECT_INTERVAL = 50; // 버프를 받는 시간 기준 (초)

    private Scoreboard scoreboard;
    private Objective objective;
    private Map<Player, Integer> hitCounts;

    // 버프 관리
    private BukkitTask buffTask;

    public FirstHitGame(EGServerMain plugin, MinigameItems item, String commandMainName, int MIN_PLAYER, int MAX_PLAYER, String displayGameName, boolean useBlockRestore) {
        super(plugin, item, commandMainName, MIN_PLAYER, MAX_PLAYER, displayGameName, useBlockRestore);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setGameRules(true, true, false, false, false);
        setGameTimeLimit(30); // 디버깅 테스트 5분 시간 제한
        hitCounts = new HashMap<>();
    }

    @Override
    public void showRules(Player player) {
        player.sendMessage("§6게임 규칙:");
        player.sendMessage("§e1. 다른 플레이어를 공격하여 점수를 획득하세요.");
        player.sendMessage("§e2. 히트 점수를 높여서 상위 랭킹에 오르세요.");
        player.sendMessage("§e3. 첫 타격을 가하는 플레이어에게 보너스 점수가 주어집니다.");
        player.sendMessage("§e4. 파워업 아이템을 활용하여 전투에서 유리한 위치를 차지하세요.");
    }

    @Override
    protected void resetGameSpecificData() {

    }


    @Override
    protected void onGameStart() {
        for (Player player : getPlayers()) {
            scores.put(player, 0);
            giveGameItems(player);

            // Use the adventure API for titles
            player.showTitle(Title.title(
                    Component.text("§a게임 시작!").color(NamedTextColor.GREEN),
                    Component.text("100점을 달성하세요!").color(NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1400), Duration.ofMillis(500))
            ));

            // Play sound using key
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        setupScoreboard();
        updateScoreboard();
        startBuffTimer();
    }

    @Override
    protected boolean loadGameData() {
        return false;
    }

    private void startBuffTimer() {
        buffTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (getState() == MinigameState.IN_PROGRESS) {
                for (Player player : getPlayers()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "보너스 효과를 받았습니다!");
                }
            }
        }, 0, BONUS_EFFECT_INTERVAL * 20L);
    }



    private void setupScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("score", "dummy",
                Component.text("선빵 게임").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    @Override
    protected void updateScoreboard() {
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        List<Map.Entry<Player, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        int rank = 1;
        for (Map.Entry<Player, Integer> entry : sortedScores) {
            Player player = entry.getKey();
            int score = entry.getValue();
            double hitRatio = getKDRatio(player);  // KDRatio 메서드를 그대로 사용하지만 이름을 변경

            // 닉네임을 일정 길이로 제한하고, 긴 경우 줄임표 사용
            String playerName = player.getName();
            if (playerName.length() > 10) {
                playerName = playerName.substring(0, 9) + "...";
            }

            // 첫 번째 줄: 플레이어 이름 및 점수
            String scoreLine = String.format("%d. %s%s %s%d",
                    rank, ChatColor.YELLOW, playerName, ChatColor.GREEN, score);

            // 두 번째 줄: Hit Ratio
            String hitRatioLine = String.format("   %s히트 점수: %.2f",
                    ChatColor.GRAY, hitRatio);

            // 순위대로 점수 설정 (높은 점수가 상단에 오도록 설정)
            objective.getScore(hitRatioLine).setScore((sortedScores.size() - rank) * 2 + 1);
            objective.getScore(scoreLine).setScore((sortedScores.size() - rank) * 2 + 2);

            // 특정 점수 이상인 플레이어에게 디버프 부여
            if (score >= DEBUFF_SCORE_THRESHOLD) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1));
                player.sendMessage(ChatColor.RED.toString() + DEBUFF_SCORE_THRESHOLD + "점수가 넘어 디버프 효과를 받았습니다!");
            }

            rank++;
            if (rank > 10) break; // 상위 10명만 표시
        }

        for (Player player : getPlayers()) {
            player.setScoreboard(scoreboard);
        }
    }
    @Override
    protected void onGameEnd() {
        if (buffTask != null) {
            buffTask.cancel();
        }
        for (Player player : getPlayers()) {
            // Use the adventure API for titles
            player.showTitle(Title.title(
                    Component.text("게임 종료!").color(NamedTextColor.RED),
                    Component.text("결과를 확인하세요.").color(NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1400), Duration.ofMillis(500))
            ));

            // Play sound using key
            player.playSound(player.getLocation(), Sound.MUSIC_DRAGON, 0.5f, 0.5f);
        }
    }

    @Override
    protected void resumeGame() {

    }


    @Override
    protected void giveGameItems(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(
                ItemUtils.createItem(Material.WOODEN_SWORD, 1, "§7§l긴 나무 검", Enchantment.KNOCKBACK, 3, "§a| §7유저를 찾아서 얼른 때리자!"));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));  // 파워업 아이템

    }
    @Override
    protected void setupGameSpecificRules() {
        // 특별한 규칙 설정이 필요 없음
    }

    // 스코어보드 삭제
    @Override
    protected void removePlayerFromScoreboard(Player player) {
        if (scoreboard != null && objective != null) {
            scoreboard.resetScores(player.getName());
        }
    }

    @Override
    public void applyCustomKillStreakBonus(Player player, int streak) {
        if (streak >= 4) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 100, 0));
        }
    }

    @Override
    public void removeCustomKillStreakEffects(Player player) {
        player.removePotionEffect(PotionEffectType.INSTANT_DAMAGE);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (getState() != MinigameState.IN_PROGRESS) return;
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        if (!getPlayers().contains(damaged) || !getPlayers().contains(damager)) return;

        if (damager.getInventory().getItemInMainHand().getType() != Material.WOODEN_SWORD) {
            damager.sendMessage("나무 검으로 플레이어를 때려야합니다.");
            return;
        }

        // 즉시 사망 처리
        event.setDamage(1000); // 매우 큰 데미지 값을 설정하여 즉시 사망하도록 함

        if (event.getDamage() > 0) {  // 데미지를 입혔을 때만 점수 증가
            int hits = hitCounts.getOrDefault(damager, 0) + 1;
            hitCounts.put(damager, hits);

            int scoreToAdd;
            switch (hits) {
                case 1:
                    scoreToAdd = 10;
                    broadcastToPlayers(Component.text("§6"+damager.getName() + " §f님이 §7" + damaged.getName() + " §f님을 §c첫번째§f로 타격했습니다! §a[ +10점 ]"));
                    break;
                case 2:
                    scoreToAdd = 7;
                    broadcastToPlayers(Component.text("§6"+damager.getName() + " §f님이 §7" + damaged.getName() + " §f님을 §c두번째§f로 타격했습니다! §a[ +7점 ]"));

                    break;
                case 3:
                    scoreToAdd = 4;
                    broadcastToPlayers(Component.text("§6"+damager.getName() + " §f님이 §7" + damaged.getName() + " §f님을 §c세번째§f로 타격했습니다! §a[ +4점 ]"));

                    break;
                default:
                    scoreToAdd = 1;
            }

            int newScore = scores.getOrDefault(damager, 0) + scoreToAdd;
            scores.put(damager, newScore);

            updateScoreboard();

            damager.sendMessage(ChatColor.GREEN + "+" + scoreToAdd + " 점을 획득했습니다! 현재 점수: " + newScore);

            if (newScore >= WIN_SCORE) {
                endGame(false);
            }
        }
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        if (killer != null) {
            broadcastToPlayers(Component.text("§c" + player.getName() + "§6님이 §c" + killer.getName() + "§e님에 의해 죽었습니다. 리스폰 시간: 3초") );
        } else {
            broadcastToPlayers(Component.text("§c" + player.getName() + "§6님이 죽었습니다. 리스폰 시간: 3초"));
        }

        if (getPlayers().contains(player)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            handlePlayerDeath(player);
        }
    }
}