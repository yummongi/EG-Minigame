package kr.egsuv.data;

import java.util.HashMap;
import java.util.Map;

public class MinigameData {
    private boolean isTeamGame;
    private int wins;
    private int losses;
    private Map<Integer, Integer> rankCounts; // 순위별 횟수 (개인전용)
    private int totalGames;
    private long lastPlayTime;
    private Map<String, Integer> teamStats; // 팀별 승리 횟수 등

    public MinigameData(boolean isTeamGame) {
        this.isTeamGame = isTeamGame;
        this.wins = 0;
        this.losses = 0;
        this.rankCounts = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            rankCounts.put(i, 0);
        }
        this.totalGames = 0;
        this.lastPlayTime = System.currentTimeMillis();
        this.teamStats = new HashMap<>();
    }

    public void addWin() {
        if (isTeamGame) {
            wins++;
        }
    }

    public void addLoss() {
        if (isTeamGame) {
            losses++;
        }
    }

    public void addTeamWin(String teamName) {
        teamStats.put(teamName, teamStats.getOrDefault(teamName, 0) + 1);
    }

    public int getTeamWins(String teamName) {
        return teamStats.getOrDefault(teamName, 0);
    }

    public void addRank(int rank) {
        if (!isTeamGame && rank >= 1 && rank <= 10) {
            rankCounts.put(rank, rankCounts.get(rank) + 1);
        }
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getRankCount(int rank) {
        return rankCounts.getOrDefault(rank, 0);
    }

    public double getWinRate() {
        int totalGames = wins + losses;
        return totalGames > 0 ? (double) wins / totalGames : 0;
    }

    public void incrementTotalGames() {
        this.totalGames++;
        this.lastPlayTime = System.currentTimeMillis();
    }

    public int getTotalGames() {
        return totalGames;
    }

    public long getLastPlayTime() {
        return lastPlayTime;
    }

    public boolean isTeamGame() {
        return isTeamGame;
    }
}