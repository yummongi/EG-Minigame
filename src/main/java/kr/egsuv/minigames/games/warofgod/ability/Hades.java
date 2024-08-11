package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.List;

public class Hades extends Ability {

    private static final int COOLDOWN_PRIMARY = 70;
    private static final int COOLDOWN_SECONDARY = 100;

    private ItemStack[] backupItems;
    private ItemStack[] backupArmor;

    public Hades(WarOfGodGame game, Player player) {
        super(game, player, "하데스");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
        this.cooldownSecondary = COOLDOWN_SECONDARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 8)) {
            startCooldown(SkillType.PRIMARY);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.5f, 0.5f);

            Player nearestEnemy = findNearestEnemy(player, 5);
            if (nearestEnemy != null) {
                dropIntoAbyss(nearestEnemy);
            }

            dropIntoAbyss(player);

        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        if (!canUseSecondarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 18)) {
            startCooldown(SkillType.SECONDARY);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.5f, 0.5f);
            player.sendTitle("", "나락과 통로를 열었습니다.", 10, 60, 20);

            for (Player enemy : findEnemiesWithinRadius(player, 5)) {
                dropIntoAbyss(enemy);
            }

        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬은 onPlayerDeath 이벤트로 처리됩니다.
    }

    @Override
    public void itemSupply() {

    }

    @EventHandler
    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().equals(this.player)) return;
        // 아테나의 패시브: 팀원이 죽을 때 레벨 1 상승
        grantTeamExperience(this.player);

        // 하데스의 패시브: 85% 확률로 아이템 보존
        if (Math.random() <= 0.85) {
            event.getDrops().clear();
            backupItems = this.player.getInventory().getContents();
            backupArmor = this.player.getInventory().getArmorContents();
        }
    }

    @EventHandler
    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.getPlayer().equals(this.player)) return;
        if (backupItems != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (game.getPlayers().contains(player)) {
                        player.getInventory().setContents(backupItems);
                        player.getInventory().setArmorContents(backupArmor);
                        player.sendTitle("", "아이템 복구 성공", 10, 60, 20);
                        backupItems = null;
                        backupArmor = null;
                    }
                }
            }.runTaskLater(game.getPlugin(), 10L);
        }
    }

    private Player findNearestEnemy(Player player, double radius) {
        List<Player> enemies = findEnemiesWithinRadius(player, radius);
        return enemies.isEmpty() ? null : enemies.get(0);
    }

    private List<Player> findEnemiesWithinRadius(Player player, double radius) {
        String playerTeam = game.getPlayerTeam(player);
        return game.getPlayers().stream()
                .filter(p -> !game.getPlayerTeam(p).equals(playerTeam) && p.getLocation().distance(player.getLocation()) <= radius)
                .toList();
    }

    private void dropIntoAbyss(Player player) {
        Location location = player.getLocation();
        location.setY(-1);
        player.teleport(location, TeleportCause.PLUGIN);
        player.sendTitle("", "나락으로 떨어집니다.", 10, 60, 20);
    }

    private void grantTeamExperience(Player deadPlayer) {
        String playerTeam = game.getPlayerTeam(deadPlayer);
        game.getPlayers().stream()
                .filter(p -> game.getPlayerTeam(p).equals(playerTeam) && !p.equals(deadPlayer))
                .forEach(p -> {
                    p.setLevel(p.getLevel() + 1);
                    p.sendTitle("", deadPlayer.getName() + " 님의 사망으로 레벨상승", 10, 60, 20);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                });
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 8개를 소비하여 5칸 이내 자신과 가장 가까운 적과 함께 나락으로 떨어집니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "코블스톤 18개를 소비하여 5칸 이내 적을 나락으로 떨어뜨립니다. (재사용 대기시간: " + COOLDOWN_SECONDARY + "초)";
    }

    @Override
    public String getPassiveDescription() {
        return "사망하여도 85% 확률로 아이템을 잃지 않습니다.";
    }
}