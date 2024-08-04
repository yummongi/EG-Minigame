package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ChatListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();


    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String prefix = getPrefix(player);
        String message = event.getMessage();
        String playerLocation = plugin.getPlayerList().get(playerUUID);

        if (playerLocation == null) {
            // 플레이어의 위치 정보가 없으면 기본값으로 "로비" 설정
            playerLocation = "로비";
        }

        String formattedMessage = ChatColor.RESET + prefix + player.getName() + " > " + message;

        event.setCancelled(true); // 기본 채팅 이벤트를 취소하고 직접 처리

        // 비동기 이벤트에서 안전하게 실행하기 위해 동기 태스크로 전환
        String finalPlayerLocation = playerLocation;
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player recipient : Bukkit.getOnlinePlayers()) {
                String recipientLocation = plugin.getPlayerList().get(recipient.getUniqueId());
                if (recipientLocation == null) {
                    recipientLocation = "로비";
                }

                // 같은 위치에 있는 플레이어에게만 메시지 전송
                if (finalPlayerLocation.equals(recipientLocation)) {
                    recipient.sendMessage(formattedMessage);
                }
            }
        });
    }

    private String getPrefix(Player player) {
        // Rank 클래스의 서버 랭크를 문자열로 반환
        return Prefix.SERVER.toString();
    }
}