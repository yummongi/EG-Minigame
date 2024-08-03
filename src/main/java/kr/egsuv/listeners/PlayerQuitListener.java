package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        event.setQuitMessage(null);
        plugin.getPlayerList().remove(player.getUniqueId());
    }
}
