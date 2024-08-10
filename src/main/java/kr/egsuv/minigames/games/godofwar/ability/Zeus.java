package kr.egsuv.minigames.games.godofwar.ability;


import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class Zeus extends Ability {

    private static final int COOLDOWN_PRIMARY = 30;
    private static final int COOLDOWN_SECONDARY = 70;

    public Zeus(WarOfGodGame game, Player player) {
        super(game, player, "제우스");
        cooldownPrimary = COOLDOWN_PRIMARY;
        cooldownSecondary = COOLDOWN_SECONDARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill() || player.isDead()) return;

        Block targetBlock = player.getTargetBlockExact(15);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendTitle("", "바라보는 곳에 블럭이 없거나 너무 멉니다.", 10, 40, 20);
            return;
        }

        if (game.takeItem(player, Material.COBBLESTONE, 5)) {
            startCooldown(SkillType.PRIMARY);
            Location targetLocation = targetBlock.getLocation();

            List<Player> enemies = game.getEnemyList(game.getPlayerTeam(player));

            // 날씨를 비로 설정
            enemies.forEach(enemy -> enemy.setPlayerWeather(WeatherType.DOWNFALL));

            enemies.forEach(enemy -> {
                if (enemy.getLocation().distance(targetLocation) < 1.5) {
                    enemy.damage(12, player);
                }
                enemy.getWorld().strikeLightningEffect(targetLocation);
            });

            // 일정 시간 후 날씨를 원래대로 복구
            new BukkitRunnable() {
                @Override
                public void run() {
                    enemies.forEach(enemy -> enemy.resetPlayerWeather());
                }
            }.runTaskLater(game.getPlugin(), 20L);  // 1초 후 복구

            player.sendTitle("", "번개가 내리칩니다.", 10, 60, 20);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);
        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }
    @Override
    public void secondarySkill() {
        if (!canUseSecondarySkill() || player.isDead()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 15)) {
            startCooldown(SkillType.SECONDARY);

            List<Player> enemies = game.getEnemyList(game.getPlayerTeam(player));

            // 날씨를 비로 설정
            enemies.forEach(enemy -> enemy.setPlayerWeather(WeatherType.CLEAR));

            player.sendTitle("", "번개가 내리칩니다.", 10, 60, 20);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);

            for (Player enemy : enemies) {
                if (enemy.getLocation().distance(player.getLocation()) <= 6) {
                    for (Player allPlayers : game.getPlayers()) {
                        allPlayers.getWorld().strikeLightningEffect(enemy.getLocation());
                    }
                    enemy.damage(12, player);
                }
            }

            // 일정 시간 후 날씨를 원래대로 복구
            new BukkitRunnable() {
                @Override
                public void run() {
                    enemies.forEach(enemy -> enemy.setPlayerWeather(WeatherType.CLEAR));
                }
            }.runTaskLater(game.getPlugin(), 20L);  // 1초 후 복구

        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 5개로 바라보는 곳에 번개를 떨굽니다.(팀에게는 피해 없음, 적에게는 12데미지) (재사용 대기시간: " + cooldownPrimary + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "코블스톤 15개를 소모하여 주변 6칸 내 적에게 번개를 떨굽니다. (재사용 대기시간: " + cooldownSecondary + "초)";
    }

    @Override
    public String getPassiveDescription() {
        return "패시브 스킬이 없습니다.";
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬 없음
    }

    @Override
    public void itemSupply() {
        // 기본 아이템 제공 없음
    }
}