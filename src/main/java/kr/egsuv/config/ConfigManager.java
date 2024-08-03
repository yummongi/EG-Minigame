package kr.egsuv.config;

import kr.egsuv.EGServerMain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private Location spawnLocation;
    private File configFile;
    private FileConfiguration config;

    public ConfigManager() {
        createConfig();
        loadSpawnLocation();
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
        saveSpawnLocation();
    }

    private void createConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("config.yml 파일을 생성할 수 없습니다: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // 기본 설정 추가
        if (!config.contains("spawn")) {
            config.set("spawn.world", "world");
            config.set("spawn.x", 0);
            config.set("spawn.y", 64);
            config.set("spawn.z", 0);
            config.set("spawn.yaw", 0);
            config.set("spawn.pitch", 0);
            saveConfig();
        }
    }

    private void loadSpawnLocation() {
        if (config.contains("spawn")) {
            World world = Bukkit.getWorld(config.getString("spawn.world"));
            double x = config.getDouble("spawn.x");
            double y = config.getDouble("spawn.y");
            double z = config.getDouble("spawn.z");
            float yaw = (float) config.getDouble("spawn.yaw");
            float pitch = (float) config.getDouble("spawn.pitch");
            spawnLocation = new Location(world, x, y, z, yaw, pitch);
        } else {
            // 기본 스폰 위치 설정
            World world = Bukkit.getWorld("world");
            spawnLocation = new Location(world, 0, 64, 0);
        }
    }

    private void saveSpawnLocation() {
        if (spawnLocation != null) {
            config.set("spawn.world", spawnLocation.getWorld().getName());
            config.set("spawn.x", spawnLocation.getX());
            config.set("spawn.y", spawnLocation.getY());
            config.set("spawn.z", spawnLocation.getZ());
            config.set("spawn.yaw", spawnLocation.getYaw());
            config.set("spawn.pitch", spawnLocation.getPitch());
            saveConfig();
        }
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("config.yml 파일을 저장할 수 없습니다: " + e.getMessage());
        }
    }
}