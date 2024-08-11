package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.games.spleef.SpleefGame;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class PlayerDamageListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            Entity damager = event.getDamager();
            Player attacker = null;

            // 공격자가 플레이어인지 또는 플레이어가 발사한 피사체인지 확인
            if (damager instanceof Player) {
                attacker = (Player) damager;
            } else if (damager instanceof Projectile) {
                Projectile projectile = (Projectile) damager;
                ProjectileSource source = projectile.getShooter();
                if (source instanceof Player) {
                    attacker = (Player) source;
                }
            }

            if (attacker == null) return;

            String victimLocation = plugin.getPlayerListLocation(victim);

            if ("로비".equals(victimLocation) || "게임로비".equals(victimLocation)) {
                event.setCancelled(true);
                return;
            }

            // 미니게임 내에서의 데미지 처리
            Minigame minigame = plugin.getCurrentGame(victim);
            if (minigame != null && minigame.getPlayers().contains(victim)) {
                // 스플리프 게임의 경우 플레이어 간 데미지 취소
                if (minigame instanceof SpleefGame) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (minigame != null && minigame.getPlayers().contains(attacker)) {
                minigame.handleDamage(attacker, victim, event);
            }
        }
    }
}