package kr.egsuv.minigames.games;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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

    public TeamDeathmatchGame(EGServerMain plugin, String commandMainName, int MIN_PLAYER, int MAX_PLAYER, String displayGameName) {
        super(plugin, commandMainName, MIN_PLAYER, MAX_PLAYER, displayGameName, true);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setGameRules(false, false, false, true, false);
        setGameTimeLimit(600); // 10분 시간 제한
    }

    @Override
    public void showRules(Player player) {

    }

    @Override
    protected void onGameStart() {
        setupScoreboard();
        assignTeams();
        for (Player player : getPlayers()) {
            giveGameItems(player);
            teleportToTeamSpawn(player);
        }
    }

    @Override
    protected void setupScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("teamdeathmatch", "dummy");
        objective.setDisplayName(ChatColor.GOLD + "팀 데스매치");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        redTeam = scoreboard.registerNewTeam("Red");
        blueTeam = scoreboard.registerNewTeam("Blue");
        redTeam.setColor(ChatColor.RED);
        blueTeam.setColor(ChatColor.BLUE);

        objective.getScore("Red Team").setScore(0);
        objective.getScore("Blue Team").setScore(0);

        for (Player player : getPlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    private void teleportToTeamSpawn(Player player) {
        String team = getPlayerTeam(player);
        List<Location> spawnLocations;
        if ("red".equals(team)) {
            spawnLocations = getConfig().getRedTeamLocations();
        } else {
            spawnLocations = getConfig().getBlueTeamLocations();
        }

        if (!spawnLocations.isEmpty()) {
            Location spawnLocation = spawnLocations.get(new Random().nextInt(spawnLocations.size()));
            player.teleport(spawnLocation);
        } else {
            player.sendMessage(ChatColor.RED + "팀 스폰 위치가 설정되지 않았습니다.");
            // 대체 위치로 텔레포트하거나 다른 처리를 할 수 있습니다.
        }
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

    @Override
    protected void giveGameItems(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(new ItemStack(Material.BOW));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 64));

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
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

    private void updateScoreboard() {
        objective.getScore("Red Team").setScore(redTeam.getEntries().size());
        objective.getScore("Blue Team").setScore(blueTeam.getEntries().size());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (getState() != MinigameState.IN_PROGRESS) return;
        if (!getPlayers().contains(victim)) return;

        event.setKeepInventory(true);
        event.getDrops().clear();

        if (killer != null && getPlayers().contains(killer) && playerTeams.get(killer) != playerTeams.get(victim)) {
            Team killerTeam = playerTeams.get(killer);
            int newScore = objective.getScore(killerTeam.getName()).getScore() + 1;
            objective.getScore(killerTeam.getName()).setScore(newScore);

            broadcastToPlayers(ChatColor.YELLOW + killer.getName() + ChatColor.WHITE + "님이 " +
                    ChatColor.YELLOW + victim.getName() + ChatColor.WHITE + "님을 처치했습니다!");

            if (newScore >= WIN_SCORE) {
                endGame();
                return;
            }
        }

        handlePlayerDeath(victim);
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