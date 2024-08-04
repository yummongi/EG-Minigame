package kr.egsuv.chat;

import org.bukkit.ChatColor;

public enum Prefix {
    BEGINNER("§7[ §8초보자 §7] " + ChatColor.RESET),
    SERVER("§7[ §aEG §7] " + ChatColor.RESET),
    FIRST_HIT("§7[ §a선빵 게임 §7] " + ChatColor.RESET);

    private String prefix;

    Prefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String toString() {
        return prefix;
    }
}
