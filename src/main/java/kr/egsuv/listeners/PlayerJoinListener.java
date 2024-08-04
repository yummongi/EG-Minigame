package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.commandList.SpawnCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private final SpawnCommand spawnCommand;

    public PlayerJoinListener(SpawnCommand spawnCommand) {
        this.spawnCommand = spawnCommand;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null);
        // 플레이어가 완전히 접속된 후 명령어를 실행하도록 1틱 지연
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean successTeleport = spawnCommand.teleportToSpawn(player);
            if (!successTeleport) {
                player.sendMessage("스폰 명령어를 실행할 수 없습니다. 관리자에게 문의하세요.");
            }

            String playerLocation = plugin.getPlayerListLocation(player);

            if (playerLocation != null && playerLocation.equals("로비")) {
                player.sendTitle("§6§lENDLESS", "§e§lMINIGAME", 10, 70, 20);

                // 로비에 있는 사람만 환영 메시지 전송
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (playerLocation.equals("로비")) {
                        onlinePlayer.sendMessage(Prefix.SERVER + player.getName() + "§f님이 로비에 입장하셨습니다.");
                    }
                }
            }
        }, 1L);
    }
}
