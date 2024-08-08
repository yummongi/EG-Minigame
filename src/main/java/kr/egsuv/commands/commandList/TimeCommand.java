package kr.egsuv.commands.commandList;

import kr.egsuv.commands.Command;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeCommand implements Command {

    @Override
    public boolean executeCommand(Player player, String[] message) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초");

        String formattedNow = now.format(formatter);

        player.sendMessage("§6현재 서버 시간: §e" + formattedNow);

        return true;
    }

/*    @Override
    public List<String> getAliases() {
        return List.of("/시간", "/서버시간", "/time");
    }*/
}
