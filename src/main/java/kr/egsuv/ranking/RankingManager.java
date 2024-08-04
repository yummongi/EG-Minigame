package kr.egsuv.ranking;

import kr.egsuv.EGServerMain;
import kr.egsuv.data.MinigameData;
import kr.egsuv.data.PlayerData;

import java.util.*;
/*
1. 팀전 점수 시스템:
   - 기본 점수: (승률 * 1000 - 패배율 * 500) * 참여도 * 최근 활동 보너스
   - 패배 페널티: 패배 횟수에 따라 지수적으로 증가하는 페널티 (PENALTY_FACTOR^패배횟수 * 1000)
   - 참여도 계수: 로그 스케일로 변경하여 초반에 빠르게 증가하고 나중에는 천천히 증가
   - 최근 활동 보너스: 15일 이내 게임 참여 시 최대 2배 보너스 (더 짧은 기간으로 조정)

2. 개인전 점수 시스템:
   - 기본 점수: 각 등수별 점수 합계 (1등: 500점, 2등: 405점, ..., 10등: 5점)
   - 평균 등수 보너스: (10 - 평균 등수)^2 * 10
   - 패배 페널티: (평균 순위 / 10)^2 * 1000 (평균 순위가 낮을수록 높은 페널티)
   - 참여도 계수: 로그 스케일로 변경
   - 최근 활동 보너스: 15일 이내 게임 참여 시 최대 2배 보너스

3. 공통 사항:
   - 최대 100게임까지만 고려하여 장기간 플레이어와 신규 플레이어 간의 격차를 줄임
   - 점수가 음수가 되지 않도록 보장

 */
public class RankingManager {
    private final EGServerMain plugin = EGServerMain.getInstance();
    private static final double PENALTY_FACTOR = 0.7; // 패배 시 점수 감소 계수
    private static final int MAX_GAMES_CONSIDERED = 30; // 최대 고려 게임 수
    private static final int RECENT_ACTIVITY_DAYS = 15; // 최근 활동으로 간주할 일 수
    private static final double MAX_RECENT_ACTIVITY_BONUS = 1.5; // 최근 활동 최대 보너스

    public List<Map.Entry<String, Double>> getTopPlayers(String gameName, int limit) {
        Map<String, Double> playerScores = new HashMap<>();

        for (UUID uuid : plugin.getDataManager().getAllPlayerUUIDs()) {
            PlayerData playerData = plugin.getDataManager().getPlayerData(uuid);
            boolean isTeamGame = plugin.getMinigameByName(gameName).isTeamGame();
            MinigameData gameData = playerData.getMinigameData(gameName, isTeamGame);

            double score = calculateScore(gameData);
            playerScores.put(playerData.getPlayerName(), score);
        }

        List<Map.Entry<String, Double>> sortedPlayers = new ArrayList<>(playerScores.entrySet());
        sortedPlayers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        return sortedPlayers.subList(0, Math.min(limit, sortedPlayers.size()));
    }

    private double calculateScore(MinigameData gameData) {
        if (gameData.isTeamGame()) {
            return calculateTeamScore(gameData);
        } else {
            return calculateIndividualScore(gameData);
        }
    }

    private double calculateTeamScore(MinigameData gameData) {
        int totalGames = Math.min(gameData.getTotalGames(), MAX_GAMES_CONSIDERED);
        if (totalGames == 0) return 0;

        double winRate = (double) gameData.getWins() / totalGames;
        double lossRate = (double) gameData.getLosses() / totalGames;
        double participationFactor = Math.log10(totalGames + 1) / Math.log10(MAX_GAMES_CONSIDERED + 1);
        double recentActivityBonus = calculateRecentActivityBonus(gameData.getLastPlayTime());

        double baseScore = (winRate * 1000 - lossRate * 500) * participationFactor * recentActivityBonus;
        double penaltyScore = Math.pow(PENALTY_FACTOR, gameData.getLosses()) * 1000;

        return Math.max(0, baseScore - penaltyScore);
    }

    private double calculateIndividualScore(MinigameData gameData) {
        int totalGames = Math.min(gameData.getTotalGames(), MAX_GAMES_CONSIDERED);
        if (totalGames == 0) return 0;

        double score = 0;
        for (int i = 1; i <= 10; i++) {
            score += gameData.getRankCount(i) * Math.pow(11 - i, 2) * 5;
        }

        double averageRank = calculateAverageRank(gameData);
        double participationFactor = Math.log10(totalGames + 1) / Math.log10(MAX_GAMES_CONSIDERED + 1);
        double recentActivityBonus = calculateRecentActivityBonus(gameData.getLastPlayTime());

        double baseScore = (score / totalGames + Math.pow(10 - averageRank, 2) * 10) * participationFactor * recentActivityBonus;
        double penaltyScore = Math.pow(averageRank / 10, 2) * 1000; // 평균 순위가 낮을수록 높은 페널티

        return Math.max(0, baseScore - penaltyScore);
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