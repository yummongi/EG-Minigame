package kr.egsuv.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandTabCompleter implements TabCompleter {

    private List<String> completions = new ArrayList<>();


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player) {
            if (args.length == 1) {
                // 첫 번째 인자에 대해 모든 명령어 반환
                setCompletions();
            }
            return completions;
        }
        return Collections.emptyList();
    }

    public void setCompletions(String... alias) {
        completions.clear();
        completions.addAll(List.of(alias));
    }

    public List<String> getCompletions() {
        return completions;
    }
}
