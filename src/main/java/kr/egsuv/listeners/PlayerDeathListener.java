package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();

        // 즉시 리스폰
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.spigot().respawn();
            player.teleport(deathLocation);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendTitle("§c사망", "§e3초 후 리스폰됩니다", 10, 40, 10);

            // 3초 후 리스폰 처리
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(plugin.getLobbyLocation());
                player.setHealth(player.getMaxHealth());
            }, 60L); // 3초
        }, 1L); // 다음 틱에 실행
    }
}