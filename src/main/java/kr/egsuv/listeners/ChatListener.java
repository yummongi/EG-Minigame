package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Rank;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String prefix = getPrefix(player);
        String message = event.getMessage();

        String formattedMessage = ChatColor.RESET + prefix + player.getName() + " > " + message;
        event.setFormat(formattedMessage);
/*        TextComponent textComponent = new TextComponent(formattedMessage);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player recipient : plugin.getServer().getOnlinePlayers()) {
                recipient.spigot().sendMessage(ChatMessageType.CHAT, textComponent);
            }
        });*/
    }

    private String getPrefix(Player player) {
        // Rank 클래스의 서버 랭크를 문자열로 반환
        return Rank.SERVER.toString();
    }
}