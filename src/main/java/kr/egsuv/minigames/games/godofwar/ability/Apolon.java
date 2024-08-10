package kr.egsuv.minigames.games.godofwar.ability;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.stream.Collectors;

public class Apolon extends Ability {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private static final int COOLDOWN_PRIMARY = 110;

    public Apolon(WarOfGodGame game, Player player) {
        super(game, player, "아폴론");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 12)) {
            startCooldown(SkillType.PRIMARY);
            game.broadcastTitle("", "태양의 힘이 강해집니다", 10, 70, 20);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);

            List<Player> enemies = game.getPlayers().stream()
                    .filter(p -> !game.getPlayerTeam(p).equals(game.getPlayerTeam(player)))
                    .collect(Collectors.toList());

            new BukkitRunnable() {
                int timeLeft = 10;

                @Override
                public void run() {
                    if (timeLeft > 0) {
                        enemies.forEach(enemy -> {
                            if (enemy.getLocation().getBlock().getLightFromSky() > 0) {
                                enemy.setFireTicks(30);
                            }
                        });
                        timeLeft--;
                    } else {
                        game.broadcastTitle("", "태양의 힘이 원래대로 돌아갔습니다", 10, 70, 20);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);

        } else {
            player.sendTitle("", "§c돌이 부족합니다", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        // 아폴론은 보조 스킬이 없습니다.
    }

    @Override
    public void passiveSkill() {
        // 아폴론의 패시브 스킬: 불에 대한 면역
    }

    @Override
    public void itemSupply() {
        player.getInventory().addItem(new ItemStack(Material.BOW, 1));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 12));
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 12개를 사용하여 모든 적을 10초간 태웁니다. (쿨타임: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "불에 대한 데미지를 입지 않습니다.";
    }

    @Override
    public void onEntityDamaged(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getEntity().equals(player)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                event.setCancelled(true);
            }
        }
    }
}