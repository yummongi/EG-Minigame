package kr.egsuv.chat;

import org.bukkit.ChatColor;

public enum Rank {
    BEGINNER("§7[ §8초보자 §7] " + ChatColor.RESET),
    SERVER("§7[ §aEG §7] " + ChatColor.RESET);

    private String rank;

    Rank(String rank) {
        this.rank = rank;
    }

    @Override
    public String toString() {
        return rank;
    }
}
