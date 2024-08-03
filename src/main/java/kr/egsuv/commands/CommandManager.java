package kr.egsuv.commands;

import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager {
    private Map<String, Command> commandMap = new HashMap<>();

    // 커맨드 명령어 등록
    public void registerCommand(Command command, String... aliases) {
        for (String alias : aliases) {
            commandMap.put(alias.toLowerCase(), command);
        }
        for (Map.Entry<String, Command> entry : commandMap.entrySet()) {
            System.out.println("명령어 등록: " + entry.getValue().getClass().getName());
        }
    }

    // 명령어 실행
    public boolean executeCommand(Player player, String commandName, String[] message) {
        Command command = commandMap.get(commandName.toLowerCase());
        if (command != null) {
            return command.executeCommand(player, message);
        }
        return false;
    }
}