package kr.egsuv.data;

import java.util.HashMap;
import java.util.Map;

public class PlayerData {
    private final String playerName;
    private final Map<String, MinigameData> minigameData;

    public PlayerData(String playerName) {
        this.playerName = playerName;
        this.minigameData = new HashMap<>();
    }

    public MinigameData getMinigameData(String gameName, boolean isTeamGame) {
        return minigameData.computeIfAbsent(gameName, k -> new MinigameData(isTeamGame));
    }

    public String getPlayerName() {
        return playerName;
    }
}