package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class PlayerQuitListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private final List<Minigame> minigames;

    public PlayerQuitListener(List<Minigame> minigames) {
        this.minigames = minigames;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        event.quitMessage(null);
        plugin.getPlayerList().remove(player.getUniqueId());

        for (Minigame minigame : minigames) {
            if (minigame.getPlayers().contains(player)) {
                minigame.handlePlayerQuit(player);
                break;
            }
        }
    }
}