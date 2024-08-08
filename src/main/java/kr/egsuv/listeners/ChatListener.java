package kr.egsuv.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.minigames.Minigame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class ChatListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String prefix = getPrefix(player);
        Component messageComponent = event.message();
        String message = PlainTextComponentSerializer.plainText().serialize(messageComponent);

        String playerLocation = plugin.getPlayerList().get(playerUUID);

        if (playerLocation == null) {
            playerLocation = "로비";
        }

        event.setCancelled(true);

        // 비동기 이벤트에서 안전하게 실행하기 위해 동기 태스크로 전환
        String finalPlayerLocation = playerLocation;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Minigame currentGame = getCurrentGame(player);
            if (currentGame != null && currentGame.isTeamGame()) {
                currentGame.handleChat(player, message);
            } else {
                String formattedMessage = "§r"+ prefix + player.getName() + " > " + message;
                for (Player recipient : Bukkit.getOnlinePlayers()) {
                    String recipientLocation = plugin.getPlayerList().get(recipient.getUniqueId());
                    if (recipientLocation == null) {
                        recipientLocation = "로비";
                    }

                    if (finalPlayerLocation.equals(recipientLocation)) {
                        recipient.sendMessage(formattedMessage);
                    }
                }
            }
        });
    }

    private String getPrefix(Player player) {
        return Prefix.SERVER.toString();
    }

    private Minigame getCurrentGame(Player player) {
        for (Minigame game : plugin.getMinigameList()) {
            if (game.getPlayers().contains(player)) {
                return game;
            }
        }
        return null;
    }
}