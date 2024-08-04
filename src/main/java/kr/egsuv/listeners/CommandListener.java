package kr.egsuv.listeners;

import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.CommandManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;

public class CommandListener implements Listener {

    private final CommandManager commandManager;

    public CommandListener(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = event.getMessage();
        String[] splitMessage = message.split(" ");
        String commandName = splitMessage[0].substring(1);
        String[] args = Arrays.copyOfRange(splitMessage, 1, splitMessage.length);

        if (message == null) {
            return;
        }

        if (commandManager.executeCommand(player, commandName, args)) {
            event.setCancelled(true);
        } else {
            player.sendMessage(Prefix.SERVER + "알 수 없는 명령어입니다.");
            event.setCancelled(true);
        }
    }

}
