package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerDamageListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            String playerLocation = plugin.getPlayerListLocation(player);
            plugin.getLogger().info(player.getName() + "의 현재 위치: " + playerLocation);
            if ("로비".equals(playerLocation) || "게임로비".equals(playerLocation)) {
                event.setCancelled(true);
            }
        }
    }
}
