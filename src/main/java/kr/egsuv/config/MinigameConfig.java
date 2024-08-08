package kr.egsuv.config;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.TeamType;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MinigameConfig {
    private final EGServerMain plugin;
    private final String minigameName;
    private File configFile;
    private FileConfiguration config;

    private static final String MAPS = "maps";
    private static final String GAME_LOBBY = "game-lobby";
    private static final String SPAWN_LOCATIONS = "spawn-locations";
    private static final String TEAM_TYPE = "team-type";
    private static final String MAP_ICONS = "map-icons";
    public MinigameConfig(EGServerMain plugin, String minigameName) {
        this.plugin = plugin;
        this.minigameName = minigameName;
        loadConfig();
    }

    private void loadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "minigames/" + minigameName + ".yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        config.options().copyDefaults(true);
        saveConfig();
    }

    public void reloadConfig() {
        loadConfig();
        plugin.getLogger().info(minigameName + "의 설정이 다시 로드되었습니다.");
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("미니게임 " + minigameName + "의 설정을 저장하는 데 실패했습니다.");
        }
    }

    public void setTeamType(TeamType teamType) {
        config.set(TEAM_TYPE, teamType.name());
        saveConfig();
    }

    public TeamType getTeamType() {
        String teamTypeName = config.getString(TEAM_TYPE, "SOLO");
        return TeamType.valueOf(teamTypeName);
    }

    public void addMap(String mapName) {
        List<String> maps = config.getStringList(MAPS);
        if (!maps.contains(mapName)) {
            maps.add(mapName);
            config.set(MAPS, maps);
            saveConfig();
        }
    }

    public List<String> getMaps() {
        return config.getStringList(MAPS);
    }

    public void setMapIcon(String mapName, ItemStack icon) {
        config.set(MAP_ICONS + "." + mapName, icon);
        saveConfig();
    }

    public ItemStack getMapIcon(String mapName) {
        return (ItemStack) config.get(MAP_ICONS + "." + mapName);
    }

    // 게임 로비 위치를 설정합니다. (맵과 상관없이 하나의 로비 위치만 존재)
    public void setGameLobby(Location location) {
        config.set(GAME_LOBBY, locationToString(location));
        saveConfig();
    }

    // 게임 로비 위치를 반환합니다.
    public Location getGameLobby() {
        String locString = config.getString(GAME_LOBBY);
        return locString != null ? stringToLocation(locString) : null;
    }


    // 특정 맵과 팀 타입에 대한 스폰 위치를 추가합니다.
    public void addSpawnLocation(String mapName, TeamType teamType, int teamNumber, Location location) {
        String key = SPAWN_LOCATIONS + "." + mapName + "." + teamType.name() + "." + teamNumber;
        List<String> locStrings = config.getStringList(key);
        locStrings.add(locationToString(location));
        config.set(key, locStrings);
        saveConfig();
    }

    public void addTeamSpawnLocation(String mapName, String teamName, Location location) {
        String key = SPAWN_LOCATIONS + "." + mapName + "." + teamName;
        List<String> locStrings = config.getStringList(key);
        locStrings.add(locationToString(location));
        config.set(key, locStrings);
        saveConfig();
    }


    // 특정 맵과 팀 타입에 대한 스폰 위치를 반환합니다.
    public List<Location> getSpawnLocations(String mapName, TeamType teamType, int teamNumber) {
        String key = SPAWN_LOCATIONS + "." + mapName + "." + teamType.name() + "." + teamNumber;
        return getLocationsFromConfig(key);
    }


    public List<Location> getTeamSpawnLocations(String mapName, String teamName) {
        String key = SPAWN_LOCATIONS + "." + mapName + "." + teamName;
        return getLocationsFromConfig(key);
    }


    public void removeSpawnLocation(String mapName, TeamType teamType, int teamNumber) {
        String key = SPAWN_LOCATIONS + "." + mapName + "." + teamType.name() + "." + teamNumber;
        config.set(key, null);
        saveConfig();
    }

    public void removeTeamSpawnLocation(String mapName, String teamName) {
        String key = SPAWN_LOCATIONS + "." + mapName + "." + teamName;
        config.set(key, null);
        saveConfig();
    }

    public void removeGameLobby() {
        config.set(GAME_LOBBY, null);
        saveConfig();
    }

    public void removeMap(String mapName) {
        List<String> maps = getMaps();
        maps.remove(mapName);
        config.set(MAPS, maps);
        config.set(GAME_LOBBY + "." + mapName, null);
        config.set(SPAWN_LOCATIONS + "." + mapName, null);
        saveConfig();
    }

    private List<Location> getLocationsFromConfig(String key) {
        List<String> locStrings = config.getStringList(key);
        List<Location> locations = new ArrayList<>();
        for (String locString : locStrings) {
            locations.add(stringToLocation(locString));
        }
        return locations;
    }

    private String locationToString(Location location) {
        return String.format("%s,%f,%f,%f,%f,%f",
                location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    private Location stringToLocation(String locString) {
        String[] parts = locString.split(",");
        return new Location(
                plugin.getServer().getWorld(parts[0]),
                Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]), Float.parseFloat(parts[5])
        );
    }

    public void setMinPlayers(int minPlayers) {
        config.set("min-players", minPlayers);
        saveConfig();
    }

    public int getMinPlayers() {
        return config.getInt("min-players", 2);
    }

    public void setMaxPlayers(int maxPlayers) {
        config.set("max-players", maxPlayers);
        saveConfig();
    }

    public int getMaxPlayers() {
        return config.getInt("max-players", 10);
    }

    // 특정 맵과 팀 이름에 대한 스폰 위치를 추가합니다.
    public void addTeamLocation(String mapName, String teamName, Location location) {
        String key = SPAWN_LOCATIONS + "." + mapName + "." + teamName;
        List<String> locStrings = config.getStringList(key);
        locStrings.add(locationToString(location));
        config.set(key, locStrings);
        saveConfig();
    }

    // 팀 위치 가져오기 (특정 맵에 대해)
    public List<Location> getTeamLocations(String mapName, String teamName) {
        String key = SPAWN_LOCATIONS + "." + mapName + "." + teamName;
        return getLocationsFromConfig(key);
    }

    // 블루팀 위치 추가 (특정 맵에 대해)
    public void addBlueTeamLocation(String mapName, Location location) {
        addTeamLocation(mapName, "blue", location);
    }

    // 레드팀 위치 추가 (특정 맵에 대해)
    public void addRedTeamLocation(String mapName, Location location) {
        addTeamLocation(mapName, "red", location);
    }

    // 블루팀 위치 가져오기 (특정 맵에 대해)
    public List<Location> getBlueTeamLocations(String mapName) {
        return getTeamLocations(mapName, "blue");
    }

    // 레드팀 위치 가져오기 (특정 맵에 대해)
    public List<Location> getRedTeamLocations(String mapName) {
        return getTeamLocations(mapName, "red");
    }
}