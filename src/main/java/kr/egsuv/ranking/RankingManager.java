package kr.egsuv.ranking;

import kr.egsuv.EGServerMain;
import kr.egsuv.data.MinigameData;
import kr.egsuv.data.PlayerData;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.TeamType;

import java.util.*;

public class RankingManager {
    private final EGServerMain plugin = EGServerMain.getInstance();
    private static final double PENALTY_FACTOR = 0.9; // 패배 시 점수 감소 계수 (약간 완화)
    private static final int MAX_GAMES_CONSIDERED = 50; // 최대 고려 게임 수 (약간 증가)
    private static final int RECENT_ACTIVITY_DAYS = 15; // 최근 활동으로 간주할 일 수
    private static final double MAX_RECENT_ACTIVITY_BONUS = 1.5; // 최근 활동 최대 보너스

    public List<Map.Entry<String, Double>> getTopPlayers(String gameName, int limit) {
        Map<String, Double> playerScores = new HashMap<>();

        for (UUID uuid : plugin.getDataManager().getAllPlayerUUIDs()) {
            PlayerData playerData = plugin.getDataManager().getPlayerData(uuid);
            Minigame minigame = plugin.getMinigameByName(gameName);
            MinigameData gameData = playerData.getMinigameData(gameName, minigame.getConfig().getTeamType() != TeamType.SOLO);

            double score = calculateScore(gameData, minigame.getConfig().getTeamType());
            playerScores.put(playerData.getPlayerName(), score);
        }

        List<Map.Entry<String, Double>> sortedPlayers = new ArrayList<>(playerScores.entrySet());
        sortedPlayers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        return sortedPlayers.subList(0, Math.min(limit, sortedPlayers.size()));
    }

    private double calculateScore(MinigameData gameData, TeamType teamType) {
        if (teamType == TeamType.SOLO) {
            return calculateSoloScore(gameData);
        } else {
            return calculateTeamScore(gameData);
        }
    }

    private double calculateSoloScore(MinigameData gameData) {
        int totalGames = Math.min(gameData.getTotalGames(), MAX_GAMES_CONSIDERED);
        if (totalGames == 0) return 0;

        double baseScore = 0;
        for (int rank = 1; rank <= 10; rank++) {
            int count = gameData.getRankCount(rank);
            baseScore += count * getPointsForRank(rank);
        }

        double averageRank = calculateAverageRank(gameData);
        double rankBonus = Math.pow(10 - averageRank, 2) * 5;
        double participationFactor = Math.log10(totalGames + 1) / Math.log10(MAX_GAMES_CONSIDERED + 1);
        double recentActivityBonus = calculateRecentActivityBonus(gameData.getLastPlayTime());

        // 패배 페널티 추가 (평균 순위가 낮을수록 높은 페널티)
        double penaltyScore = Math.pow(averageRank / 10, 2) * 500;

        double finalScore = ((baseScore / totalGames) + rankBonus) * participationFactor * recentActivityBonus - penaltyScore;
        return Math.max(0, finalScore);
    }

    private double calculateTeamScore(MinigameData gameData) {
        int totalGames = Math.min(gameData.getTotalGames(), MAX_GAMES_CONSIDERED);
        if (totalGames == 0) return 0;

        double winRate = (double) gameData.getWins() / totalGames;
        double lossRate = (double) gameData.getLosses() / totalGames;
        double participationFactor = Math.log10(totalGames + 1) / Math.log10(MAX_GAMES_CONSIDERED + 1);
        double recentActivityBonus = calculateRecentActivityBonus(gameData.getLastPlayTime());

        double baseScore = (winRate * 1000 - lossRate * 300) * participationFactor * recentActivityBonus;

        // 패배 페널티 조정
        double penaltyScore = (1 - Math.pow(PENALTY_FACTOR, gameData.getLosses())) * 500;

        return Math.max(0, baseScore - penaltyScore);
    }

    private int getPointsForRank(int rank) {
        switch (rank) {
            case 1: return 500;
            case 2: return 400;
            case 3: return 300;
            case 4: return 250;
            case 5: return 200;
            case 6: return 150;
            case 7: return 100;
            case 8: return 75;
            case 9: return 50;
            case 10: return 25;
            default: return 0;
        }
    }

    private double calculateAverageRank(MinigameData gameData) {
        int totalRanks = 0;
        int totalGames = 0;
        for (int i = 1; i <= 10; i++) {
            int count = gameData.getRankCount(i);
            totalRanks += i * count;
            totalGames += count;
        }
        return totalGames > 0 ? (double) totalRanks / totalGames : 0;
    }

    private double calculateRecentActivityBonus(long lastPlayTime) {
        long daysSinceLastPlay = (System.currentTimeMillis() - lastPlayTime) / (1000 * 60 * 60 * 24);
        return Math.max(1.0, MAX_RECENT_ACTIVITY_BONUS - (daysSinceLastPlay / (double)RECENT_ACTIVITY_DAYS) * (MAX_RECENT_ACTIVITY_BONUS - 1.0));
    }
}