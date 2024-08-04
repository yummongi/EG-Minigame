package kr.egsuv.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.egsuv.EGServerMain;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;

public class DataManager {
    private final EGServerMain plugin = EGServerMain.getInstance();
    private final Map<UUID, PlayerData> playerDataMap;
    private final File dataFolder;
    private final Gson gson;

    public DataManager() {
        this.playerDataMap = new HashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), k -> loadPlayerData(player));
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, k -> loadPlayerData(uuid));
    }

    private PlayerData loadPlayerData(Player player) {
        File file = new File(dataFolder, player.getUniqueId().toString() + ".json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                return gson.fromJson(reader, PlayerData.class);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load player data for " + player.getName());
                e.printStackTrace();
            }
        }
        return new PlayerData(player.getName());
    }

    private PlayerData loadPlayerData(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                return gson.fromJson(reader, PlayerData.class);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load player data for UUID: " + uuid);
                e.printStackTrace();
            }
        }
        return new PlayerData(Bukkit.getOfflinePlayer(uuid).getName());
    }

    public void savePlayerData(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            File file = new File(dataFolder, player.getUniqueId() + ".json");
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(data, writer);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save player data for " + player.getName());
                e.printStackTrace();
            }
        }
    }

    public void saveAllPlayerData() {
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            File file = new File(dataFolder, entry.getKey() + ".json");
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(entry.getValue(), writer);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save player data for " + entry.getValue().getPlayerName());
                e.printStackTrace();
            }
        }
    }

    public Set<UUID> getAllPlayerUUIDs() {
        Set<UUID> uuids = new HashSet<>();
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String uuidString = fileName.substring(0, fileName.length() - 5); // Remove .json
                try {
                    uuids.add(UUID.fromString(uuidString));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in filename: " + fileName);
                }
            }
        }
        return uuids;
    }
}