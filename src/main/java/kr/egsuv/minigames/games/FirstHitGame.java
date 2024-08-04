package kr.egsuv.minigames.games;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class FirstHitGame extends Minigame implements Listener {

    private static final int WIN_SCORE = 10;
    private Scoreboard scoreboard;
    private Objective objective;

    public FirstHitGame(EGServerMain plugin, String commandMainName, int MIN_PLAYER, int MAX_PLAYER, String displayGameName, boolean isTeamGame) {
        super(plugin, commandMainName, MIN_PLAYER, MAX_PLAYER, displayGameName, false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setGameRules(false, false, false, false, false);
        setGameTimeLimit(300); // 5분 시간 제한
    }

    @Override
    public void showRules(Player player) {

    }

    @Override
    protected void onGameStart() {
        setupScoreboard();
        for (Player player : getPlayers()) {
            scores.put(player, 0);
            giveGameItems(player);
        }
    }

    @Override
    protected void setupScoreboard() {
        scoreboard = plugin.getServer().getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("firsthit", "dummy");
        objective.setDisplayName(ChatColor.GOLD + "선빵 게임");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (Player player : getPlayers()) {
            Score score = objective.getScore(player.getName());
            score.setScore(0);
            player.setScoreboard(scoreboard);
        }
    }

/*    @Override
    protected void onGameEnd() {
        super.showFinalRanking();
        for (Player player : getPlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        broadcastToServer(Prefix.SERVER.toString() + ChatColor.YELLOW + getDisplayGameName() + " 게임이 종료되었습니다!");
    }*/

    @Override
    protected void giveGameItems(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(new ItemStack(Material.WOOD_SWORD));
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

    private void updateScoreboard() {
        for (Player player : getPlayers()) {
            Score score = objective.getScore(player.getName());
            score.setScore(scores.getOrDefault(player, 0));
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (getState() != MinigameState.IN_PROGRESS) return;
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        if (!getPlayers().contains(damaged) || !getPlayers().contains(damager)) return;

        int newScore = scores.getOrDefault(damager, 0) + 1;
        scores.put(damager, newScore);
        updateScoreboard();

        damager.sendMessage(ChatColor.GREEN + "+" + 1 + " 점을 획득했습니다! 현재 점수: " + newScore);

        if (newScore >= WIN_SCORE) {
            endGame();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (getPlayers().contains(player)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            handlePlayerDeath(player);
        }
    }
}