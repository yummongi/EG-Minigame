package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.games.spleef.SpleefGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.time.Duration;

public class PlayerDeathListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.deathMessage(null);
        Player victim = event.getEntity();

        // 플레이어가 게임 중인지 확인
        Minigame currentGame = plugin.getCurrentGame(victim);
        if (currentGame != null) {
            // 게임 중인 경우, 게임의 handlePlayerDeath 메소드 호출
            currentGame.handlePlayerDeath(victim);
            // 스플리프 게임의 경우 즉시 리스폰
            if (currentGame instanceof SpleefGame) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    victim.spigot().respawn();
                }, 1L);
            }
        } else {
            // 게임 중이 아닌 경우, 기본 리스폰 로직 적용
            handleNormalDeath(victim, victim.getLocation());
        }
    }

    private void handleNormalDeath(Player player, Location deathLocation) {
        // 즉시 리스폰
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.spigot().respawn();
            player.teleport(deathLocation);
            player.setGameMode(GameMode.SPECTATOR);
            player.showTitle(Title.title(
                    Component.text("사망").color(NamedTextColor.RED),
                    Component.text("3초 후 리스폰됩니다").color(NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));

            // 3초 후 리스폰 처리
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(plugin.getLobbyLocation());
                player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            }, 60L); // 3초
        }, 1L); // 다음 틱에 실행
    }
}