package kr.egsuv.config;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.TeamType;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MinigameConfig {

    private final EGServerMain plugin;
    private final String minigameName;
    private File configFile;
    private FileConfiguration config;

    private static final String BLUE_LOCATIONS = "blue-locations";
    private static final String RED_LOCATIONS = "red-locations";
    private static final String TEAM_LOCATIONS = "team-locations";
    private static final String TEAM_SPAWN_LOCATIONS = "team-spawn-locations";
    private static final String SOLO_LOCATIONS = "solo-locations";
    private static final String LOBBY_LOCATION = "lobby-location";
    private static final String GAME_LOBBY_LOCATION = "game-lobby-location";

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

        // 기본 설정값 저장
        config.addDefault("min-players", 2);
        config.addDefault("max-players", 10);
        config.addDefault("game-locations", new ArrayList<String>());

        config.options().copyDefaults(true);
        saveConfig();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("미니게임 " + minigameName + "의 설정을 저장하는 데 실패했습니다.");
        }
    }

    public void addTeamSpawnLocation(String teamName, int teamNumber, Location location) {
        String key = TEAM_SPAWN_LOCATIONS + "." + teamName + "." + teamNumber;
        List<String> locStrings = config.getStringList(key);
        locStrings.add(locationToString(location));
        config.set(key, locStrings);
        saveConfig();
    }

    public List<Location> getTeamSpawnLocations(String teamName, int teamNumber) {
        String key = TEAM_SPAWN_LOCATIONS + "." + teamName + "." + teamNumber;
        return getLocationsFromConfig(key);
    }

    public void setTeamLocation(String teamName, Location location) {
        addLocationToConfig(TEAM_LOCATIONS + "." + teamName, location);
    }

    public List<Location> getTeamLocations(String teamName) {
        return getLocationsFromConfig(TEAM_LOCATIONS + "." + teamName);
    }

    public void setTeamType(TeamType teamType) {
        config.set("team-type", teamType.name());
        saveConfig();
    }

    public TeamType getTeamType() {
        String teamTypeName = config.getString("team-type", "SOLO");
        return TeamType.valueOf(teamTypeName);
    }

    public void setNumberOfTeams(int numberOfTeams) {
        config.set("number-of-teams", numberOfTeams);
        saveConfig();
    }

    public int getNumberOfTeams() {
        return config.getInt("number-of-teams", 1);
    }

    public void setLobbyLocation(Location location) {
        config.set(LOBBY_LOCATION, locationToString(location));
        saveConfig();
    }

    public void setGameLobbyLocation(Location location) {
        config.set(GAME_LOBBY_LOCATION, locationToString(location));
        saveConfig();
    }

    public Location getGameLobbyLocation() {
        String locString = config.getString(GAME_LOBBY_LOCATION);
        return locString != null ? stringToLocation(locString) : null;
    }


    public void removeSoloLocation(int index) {
        List<String> locStrings = config.getStringList(SOLO_LOCATIONS);
        if (index >= 0 && index < locStrings.size()) {
            locStrings.remove(index);
            config.set(SOLO_LOCATIONS, locStrings);
            saveConfig();
        } else {
            throw new IndexOutOfBoundsException("Invalid index for solo location removal");
        }
    }

    public List<Location> getBlueTeamLocations() {
        return getLocationsFromConfig(BLUE_LOCATIONS);
    }

    public List<Location> getRedTeamLocations() {
        return getLocationsFromConfig(RED_LOCATIONS);
    }

    public List<Location> getSoloLocations() {
        return getLocationsFromConfig(SOLO_LOCATIONS);
    }



    private List<Location> getLocationsFromConfig(String key) {
        List<String> locStrings = config.getStringList(key);
        List<Location> locations = new ArrayList<>();
        for (String locString : locStrings) {
            locations.add(stringToLocation(locString));
        }
        return locations;
    }

    public void addBlueTeamLocation(Location location) {
        addLocationToConfig(BLUE_LOCATIONS, location);
    }

    public void addRedTeamLocation(Location location) {
        addLocationToConfig(RED_LOCATIONS, location);
    }

    public void addSoloLocation(Location location) {
        addLocationToConfig(SOLO_LOCATIONS, location);
    }

    private void addLocationToConfig(String key, Location location) {
        List<String> locStrings = config.getStringList(key);
        locStrings.add(locationToString(location));
        config.set(key, locStrings);
        saveConfig();
    }

    private String locationToString(Location location) {
        return String.format("%s,%f,%f,%f,%f,%f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    public int getMinPlayers() {
        return config.getInt("min-players");
    }

    public void setMinPlayers(int minPlayers) {
        config.set("min-players", minPlayers);
        saveConfig();
    }

    public int getMaxPlayers() {
        return config.getInt("max-players");
    }

    public void setMaxPlayers(int maxPlayers) {
        config.set("max-players", maxPlayers);
        saveConfig();
    }

    public Location getLobbyLocation() {
        // ConfigManager에서 스폰 위치를 가져옵니다.
        return plugin.getSpawnConfigManager().getSpawnLocation();
    }

    public List<Location> getGameLocations() {
        List<String> locStrings = config.getStringList("game-locations");
        List<Location> locations = new ArrayList<>();
        for (String locString : locStrings) {
            String[] parts = locString.split(",");
            locations.add(new Location(
                    plugin.getServer().getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5])
            ));
        }
        return locations;
    }

    private Location stringToLocation(String locString) {
        String[] parts = locString.split(",");
        return new Location(
                plugin.getServer().getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
        );
    }

    public void addGameLocation(Location location) {
        List<String> locStrings = config.getStringList("game-locations");
        String locString = String.format("%s,%f,%f,%f,%f,%f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
        locStrings.add(locString);
        config.set("game-locations", locStrings);
        saveConfig();
    }

    public void removeGameLocation(int index) {
        List<String> locStrings = config.getStringList("game-locations");
        if (index >= 0 && index < locStrings.size()) {
            locStrings.remove(index);
            config.set("game-locations", locStrings);
            saveConfig();
        }
    }

    // 추가적인 게임별 설정을 위한 메서드들을 여기에 구현할 수 있습니다.
}