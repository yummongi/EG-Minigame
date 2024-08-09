package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.games.SpleefGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerDamageListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            String victimLocation = plugin.getPlayerListLocation(victim);

            if ("로비".equals(victimLocation) || "게임로비".equals(victimLocation)) {
                event.setCancelled(true);
                return;
            }

            // 미니게임 내에서의 데미지 처리
            Minigame minigame = plugin.getCurrentGame(victim);
            if (minigame != null && minigame.getPlayers().contains(attacker)) {
                // 스플리프 게임의 경우 플레이어 간 데미지 취소
                if (minigame instanceof SpleefGame) {
                    event.setCancelled(true);
                    return;
                }

                if (event.getDamage() > 0) {
                    minigame.handleDamage(attacker, victim, event.getFinalDamage());
                }
            }
        }
    }
}