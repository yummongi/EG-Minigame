package kr.egsuv.minigames;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MinigamePenaltyManager {
    // 데이터 정리 주기
    private static final long CLEANUP_INTERVAL = 3600000; // 1시간 (밀리초 단위)
    private static final long PENALTY_DATA_RETENTION = 86400000; // 24시간 (밀리초 단위)


    private static final long RECONNECT_WINDOW = 300000; // 5분
    private static final long PENALTY_DURATION = 600000; // 10분

    private static Map<UUID, Long> disconnectTimes = new HashMap<>();
    private static Map<UUID, Long> penaltyTimes = new HashMap<>();
    private static Map<UUID, String> lastGamePlayed = new HashMap<>();

    public static void handlePlayerDisconnect(Player player, String gameName) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        disconnectTimes.put(playerId, currentTime);
        lastGamePlayed.put(playerId, gameName);

    }

    public static boolean handlePlayerReconnect(Player player, String gameName) {
        UUID playerId = player.getUniqueId();
        Long disconnectTime = disconnectTimes.get(playerId);
        long currentTime = System.currentTimeMillis();

        if (disconnectTime != null) {
            if (currentTime - disconnectTime < RECONNECT_WINDOW) {
                if (lastGamePlayed.get(playerId).equals(gameName)) {
                    disconnectTimes.remove(playerId);
                    lastGamePlayed.remove(playerId);
                    return true;
                }
            }
            penaltyTimes.put(playerId, currentTime + PENALTY_DURATION);
            player.sendMessage(Prefix.SERVER + "§f재접속 시간이 초과되거나 인원이 부족하여 게임을 종료시킨 경우 10분 동안 모든 게임 참여가 제한됩니다.");
        }
        disconnectTimes.remove(playerId);
        lastGamePlayed.remove(playerId);
        return false;
    }

    public static boolean canJoinGame(Player player, String gameName) {
        UUID playerId = player.getUniqueId();
        Long penaltyEndTime = penaltyTimes.get(playerId);
        Long disconnectTime = disconnectTimes.get(playerId);
        long currentTime = System.currentTimeMillis();

        if (penaltyEndTime != null && currentTime < penaltyEndTime) {
            long remainingTime = (penaltyEndTime - currentTime) / 60000;
            player.sendMessage(Prefix.SERVER + "탈주 패널티 적용으로 게임 참여 제한 중입니다. 남은 시간: " + remainingTime + "분");
            return false;
        }

        if (disconnectTime != null) {
            if (currentTime - disconnectTime < RECONNECT_WINDOW) {
                String lastGame = lastGamePlayed.get(playerId);
                if (!lastGame.equals(gameName)) {
                    player.sendMessage(Prefix.SERVER + "5분 동안은 " + lastGame + " 게임에만 참여할 수 있습니다.");
                    return false;
                }
            } else {
                penaltyTimes.put(playerId, currentTime + PENALTY_DURATION);
                disconnectTimes.remove(playerId);
                lastGamePlayed.remove(playerId);
                player.sendMessage(Prefix.SERVER + "재접속 시간이 초과되어 10분 동안 모든 게임 참여가 제한됩니다.");
                return false;
            }
        }

        return true;
    }

    public static void clearPlayerData(UUID playerId) {
        disconnectTimes.remove(playerId);
        penaltyTimes.remove(playerId);
        lastGamePlayed.remove(playerId);
    }

    public static void applyPenaltyForAbnormalEnd(UUID playerId) {
        penaltyTimes.put(playerId, System.currentTimeMillis() + PENALTY_DURATION);
        disconnectTimes.remove(playerId);
        lastGamePlayed.remove(playerId);
    }

    public static void startCleanupTask(EGServerMain plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                disconnectTimes.entrySet().removeIf(entry -> currentTime - entry.getValue() > RECONNECT_WINDOW);
                penaltyTimes.entrySet().removeIf(entry -> currentTime > entry.getValue() + PENALTY_DATA_RETENTION);
                lastGamePlayed.entrySet().removeIf(entry -> !disconnectTimes.containsKey(entry.getKey()));
            }
        }.runTaskTimer(plugin, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    public static boolean wasPlayerInGame(UUID playerId, String gameName) {
        String lastGame = lastGamePlayed.get(playerId);
        return lastGame != null && lastGame.equals(gameName);
    }

    public static Map<UUID, Long> getDisconnectTimes() {
        return disconnectTimes;
    }

    public static Map<UUID, String> getLastGamePlayed() {
        return lastGamePlayed;
    }
}