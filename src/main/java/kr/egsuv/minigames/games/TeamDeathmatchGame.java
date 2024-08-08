package kr.egsuv.minigames.games;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameItems;
import kr.egsuv.minigames.MinigameState;
import kr.egsuv.minigames.TeamType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TeamDeathmatchGame extends Minigame implements Listener {

    private static final int WIN_SCORE = 30;
    private Scoreboard scoreboard;
    private Objective objective;
    private Team redTeam;
    private Team blueTeam;
    private Map<Player, Team> playerTeams = new HashMap<>();

    /*
    (TeamType.DUO, 3): 2명씩 3팀, 총 6명
    (TeamType.TRIPLE, 2): 3명씩 2팀, 총 6명
    (TeamType.SOLO, 10): 1명씩 10팀, 총 10명
     */

    public TeamDeathmatchGame(EGServerMain plugin, MinigameItems item, String commandMainName, int MIN_PLAYER, int MAX_PLAYER, String displayGameName, boolean isTeamGame, TeamType teamType, int numberOfTeams, boolean isRedBlueTeamGame, boolean useBlockRestore) {
        super(plugin, item,  commandMainName, MIN_PLAYER, MAX_PLAYER, displayGameName, isTeamGame, teamType, numberOfTeams, isRedBlueTeamGame, useBlockRestore);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setGameRules(false, false, false, true, false);
        setGameTimeLimit(300); // 5분 시간 제한
    }

    @Override
    public void applyCustomKillStreakBonus(Player player, int streak) {
        if (streak == 5) {
            healNearbyTeammates(player);
            broadcastToPlayers(Component.text(Prefix.SERVER + getPlayerTeam(player) + "팀의 " + player.getName() + "님이 연속 5킬로 인해 근처 10칸 범위에 같은 팀 유저를 회복시킵니다.") );
        }
    }

    private void healNearbyTeammates(Player player) {
        String team = getPlayerTeam(player);
        for (Player teammate : player.getWorld().getPlayers()) {
            if (getPlayerTeam(teammate).equals(team) && teammate.getLocation().distance(player.getLocation()) <= 10) {
                teammate.setHealth(Math.min(teammate.getHealth() + 4, teammate.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
            }
        }
    }

    @Override
    public void removeCustomKillStreakEffects(Player player) {

    }

    @Override
    public void showRules(Player player) {

    }

    @Override
    protected void resetGameSpecificData() {

    }

    @Override
    protected void onGameStart() {
        super.setupTeamsAndScoreboard();
        for (Player player : getPlayers()) {
            giveGameItems(player);
        }
    }

    @Override
    protected void onGameEnd() {

    }


/*
    @Override
    protected void onGameEnd() {
        super.showFinalRanking();
        for (Player player : getPlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        broadcastToServer(Prefix.SERVER.toString() + ChatColor.YELLOW + getDisplayGameName() + " 게임이 종료되었습니다!");
    }
*/

    // 기본 템 및 리스폰 마다 지급이 됨
    @Override
    protected void giveGameItems(Player player) {
        String teamName = getPlayerTeam(player);
        giveColoredArmor(player, teamName); // 색상 갑옷 지급
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(new ItemStack(Material.BOW));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
    }

    @Override
    protected void setupGameSpecificRules() {
        // 팀 데스매치 특정 규칙 설정
    }

    @Override
    protected void removePlayerFromScoreboard(Player player) {
        if (scoreboard != null) {
            Team team = playerTeams.get(player);
            if (team != null) {
                team.removeEntry(player.getName());
            }
            playerTeams.remove(player);
        }
    }

    @Override
    protected void updateScoreboard() {
        for (String teamName : teams.keySet()) {
            int teamScore = teams.get(teamName).stream()
                    .mapToInt(player -> scores.getOrDefault(player, 0))
                    .sum();
            updateScore(teamName, teamScore);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (getState() != MinigameState.IN_PROGRESS) return;
        if (!getPlayers().contains(victim)) return;

        event.setKeepInventory(true);
        event.getDrops().clear();

        if (killer != null && getPlayers().contains(killer) && !getPlayerTeam(killer).equals(getPlayerTeam(victim))) {
            String killerTeam = getPlayerTeam(killer);
            int newScore = objective.getScore(killerTeam.toUpperCase()).getScore() + 1;
            updateScore(killerTeam, newScore);

            broadcastToPlayers(Component.text(ChatColor.YELLOW + killer.getName() + ChatColor.WHITE + "님이 " +
                    ChatColor.YELLOW + victim.getName() + ChatColor.WHITE + "님을 처치했습니다!"));

            if (newScore >= WIN_SCORE) {
                endGame(false);
                return;
            }
        }

        handlePlayerDeath(victim);
        updateScoreboard();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (getState() != MinigameState.IN_PROGRESS) return;
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        if (!getPlayers().contains(damaged) || !getPlayers().contains(damager)) return;

        if (getPlayerTeam(damaged).equals(getPlayerTeam(damager))) {
            event.setCancelled(true);
            damager.sendMessage(ChatColor.RED + "팀원을 공격할 수 없습니다!");
        }
    }
}