package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Uros extends Ability {

    private static final int COOLDOWN_PRIMARY = 60;
    private boolean isGodMode = false;

    public Uros(WarOfGodGame game, Player player) {
        super(game, player, "우로스");
        cooldownPrimary = COOLDOWN_PRIMARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill() || player.isDead()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 8)) {
            startCooldown(SkillType.PRIMARY);
            activateGodMode();
        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {

    }

    private void activateGodMode() {
        isGodMode = true;
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
        player.sendTitle("§c5", "산의 가호가 사라지기까지", 10, 60, 20);

        new BukkitRunnable() {
            int timeLeft = 5;

            @Override
            public void run() {
                if (timeLeft > 1) {
                    timeLeft--;
                    player.sendTitle("§c" + timeLeft, "산의 가호가 사라지기까지", 10, 60, 20);
                } else {
                    isGodMode = false;
                    player.sendTitle("", "산의 가호를 잃었습니다.", 10, 60, 20);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.2f, 1.8f);
                    this.cancel();
                }
            }
        }.runTaskTimer(game.getPlugin(), 20L, 20L);
    }


    @EventHandler
    @Override
    public void onEntityDamaged(EntityDamageEvent event) {
        if (!event.getEntity().equals(this.player)) return;
        if (isGodMode) {
            event.setCancelled(true);
            this.player.getWorld().playSound(this.player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.2f, 1.8f);
        }
    }

    @EventHandler
    @Override
    public void onHitted(EntityDamageByEntityEvent event) {
        if (!event.getEntity().equals(this.player)) return;
        if (!isGodMode && (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE)) {
            event.setCancelled(true);
            this.player.damage(event.getDamage());
        }
    }

    @Override
    public String getPrimaryDescription() {
        return "돌 8개를 사용하여 5초간 무적 상태가 됩니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "일반 타격으로는 넉백을 받지 않습니다.";
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