package kr.egsuv.commands.commandList;

import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.Command;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class WhisperCommand implements Command {

    @Override
    public boolean executeCommand(Player sender, String[] message) {
        if (message.length < 2) {
            sender.sendMessage(Component.text(Prefix.SERVER + "§c사용법: /w <플레이어> <메시지>"));
            return true;
        }

        // 메시지에서 첫 번째 인자는 수신자 이름이므로 분리
        String recipientName = message[0];
        Player recipient = Bukkit.getPlayer(recipientName);

        if (recipient == null || !recipient.isOnline()) {
            sender.sendMessage(Component.text(Prefix.SERVER + "§c해당 플레이어를 찾을 수 없습니다."));
            return true;
        }

        // 수신자가 존재할 경우 메시지를 생성
        StringBuilder msgBuilder = new StringBuilder();
        for (int i = 1; i < message.length; i++) {
            msgBuilder.append(message[i]).append(" ");
        }
        String msg = msgBuilder.toString().trim();

        // 발신자와 수신자에게 메시지 전달
        sender.sendMessage(Component.text(Prefix.SERVER +"§7[귓속말] §f" + recipient.getName() + ": " + msg));
        recipient.sendMessage(Component.text(Prefix.SERVER + "§7[귓속말] §f" + sender.getName() + ": " + msg));

        return true;
    }
}