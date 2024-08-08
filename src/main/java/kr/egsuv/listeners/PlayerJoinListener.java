package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.commandList.SpawnCommand;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigamePenaltyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;
import java.util.List;

public class PlayerJoinListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private final SpawnCommand spawnCommand;
    private final List<Minigame> minigames;

    public PlayerJoinListener(SpawnCommand spawnCommand, List<Minigame> minigames) {
        this.spawnCommand = spawnCommand;
        this.minigames = minigames;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(null);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean successTeleport = spawnCommand.teleportToSpawn(player);
            if (!successTeleport) {
                player.sendMessage("스폰 명령어를 실행할 수 없습니다. 관리자에게 문의하세요.");
            }

            String playerLocation = plugin.getPlayerListLocation(player);

            for (Minigame minigame : minigames) {
                minigame.handlePlayerReconnect(player);
            }

            if ("로비".equals(playerLocation)) {
                player.showTitle(Title.title(
                        Component.text("§6§lENDLESS").color(NamedTextColor.GOLD),
                        Component.text("§e§lMINIGAME").color(NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1400), Duration.ofMillis(500))
                ));

                // Send welcome message to players in the lobby
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    String onlinePlayerLocation = plugin.getPlayerListLocation(onlinePlayer);
                    if ("로비".equals(onlinePlayerLocation)) {
                        onlinePlayer.sendMessage(Prefix.SERVER + player.getName() + "§f님이 로비에 입장하셨습니다.");
                    }
                }
            }
        }, 1L);
    }
}
