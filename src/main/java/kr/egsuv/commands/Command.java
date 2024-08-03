package kr.egsuv.commands;

import org.bukkit.entity.Player;

import java.util.List;

public interface Command {
    boolean executeCommand(Player player, String[] message);

//    List<String> getAliases();
}
