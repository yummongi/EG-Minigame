package kr.egsuv.data;
import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.config.MinigameConfig;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameState;
import kr.egsuv.minigames.TeamType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BlockRestoreManager implements Listener {
    private final EGServerMain plugin = EGServerMain.getInstance();
    private final Map<String, Map<ChunkPosition, Set<BlockChangeRecord>>> worldChanges;
    private final Map<String, Map<String, CuboidRegion>> gameMapRegions;
    private final Map<Player, Location> pos1Map;
    private final Map<Player, Location> pos2Map;
    private final ReentrantLock restoreLock;
    private YamlConfiguration config;
    private int blocksPerTick;
    private boolean isRestoring;
    private BukkitTask restorationTask;

    private final Map<String, Map<String, Location>> gameMapCenters = new ConcurrentHashMap<>();

    private static final Set<Material> LIQUID_MATERIALS = EnumSet.of(
            Material.WATER, Material.LAVA
    );

    // 폴더 및 파일 관리
    private final File blockRestoreFolder;
    private final File configFile;
    private final File dataFile;
    private final File initialStatesFolder;
    private final File blockChangesFolder;

    // 초기 저장된 맵
    private final Map<String, Map<String, Map<SerializableLocation, SerializableBlockState>>> initialBlockStates = new ConcurrentHashMap<>();

    private final AtomicInteger totalBlocksToRestore = new AtomicInteger(0);
    private final AtomicInteger blocksRestored = new AtomicInteger(0);




    public BlockRestoreManager() {
        this.blockRestoreFolder = new File(plugin.getDataFolder(), "block_restore");
        if (!blockRestoreFolder.exists()) {
            blockRestoreFolder.mkdirs();
        }
        this.configFile = new File(blockRestoreFolder, "block_restore_config.yml");
        this.dataFile = new File(blockRestoreFolder, "block_restore_data.yml");
        this.initialStatesFolder = new File(blockRestoreFolder, "initial_states");
        if (!initialStatesFolder.exists()) {
            initialStatesFolder.mkdirs();
        }
        this.blockChangesFolder = new File(blockRestoreFolder, "block_changes");
        if (!blockChangesFolder.exists()) {
            blockChangesFolder.mkdirs();
        }

        this.worldChanges = new ConcurrentHashMap<>();
        this.gameMapRegions = new ConcurrentHashMap<>();
        this.pos1Map = new ConcurrentHashMap<>();
        this.pos2Map = new ConcurrentHashMap<>();
        this.restoreLock = new ReentrantLock();
        this.isRestoring = false;

        loadConfig();
        loadAllInitialStates();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadRestoreData();
    }


    private void loadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        blocksPerTick = config.getInt("blocksPerTick", 50);
        if (!configFile.exists()) {
            config.set("blocksPerTick", blocksPerTick);
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "설정 파일 저장 중 오류 발생", e);
            }
        }
    }

    public void setBlocksPerTick(int blocksPerTick) {
        this.blocksPerTick = blocksPerTick;
        config.set("blocksPerTick", blocksPerTick);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "설정 파일 저장 중 오류 발생", e);
        }
    }



    public void logBlockChange(Block block, boolean isPlaced, String gameName, String mapName) {
        if (!isInRestoreRegion(gameName, mapName, block.getLocation())) {
            return;
        }

        ChunkPosition chunkPos = new ChunkPosition(block.getChunk());
        SerializableBlockState initialState = getInitialBlockState(gameName, mapName, block.getLocation());
        BlockChangeRecord change = new BlockChangeRecord(block, isPlaced, initialState);

        worldChanges.computeIfAbsent(mapName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkPos, k -> ConcurrentHashMap.newKeySet())
                .add(change);

        plugin.getLogger().info("블록 변경 기록: " + gameName + ", " + mapName + ", " +
                (isPlaced ? "설치: " : "파괴: ") + block.getType() +
                " at " + formatLocation(block.getLocation()));

        if (worldChanges.get(mapName).get(chunkPos).size() > 1000) {
            saveChunkChanges(mapName, chunkPos);
        }
    }


    private SerializableBlockState getInitialBlockState(String gameName, String mapName, Location location) {
        return initialBlockStates
                .getOrDefault(gameName, Collections.emptyMap())
                .getOrDefault(mapName, Collections.emptyMap())
                .get(new SerializableLocation(location));
    }
    public void startRestoration(Minigame minigame, String mapName) {
        if (!restoreLock.tryLock()) {
            plugin.getLogger().warning("이미 복구 작업이 진행 중입니다.");
            return;
        }

        try {
            isRestoring = true;
            if (mapName == null) {
                plugin.getLogger().warning("맵 이름이 null입니다. 복구를 진행할 수 없습니다.");
                minigame.onRestorationComplete();
                return;
            }

            Map<ChunkPosition, Set<BlockChangeRecord>> chunkChanges = worldChanges.getOrDefault(mapName, new ConcurrentHashMap<>());
            Map<SerializableLocation, SerializableBlockState> initialStates = initialBlockStates
                    .getOrDefault(minigame.getCOMMAND_MAIN_NAME(), Collections.emptyMap())
                    .getOrDefault(mapName, Collections.emptyMap());

            CuboidRegion region = gameMapRegions.getOrDefault(minigame.getCOMMAND_MAIN_NAME(), Collections.emptyMap()).get(mapName);
            if (region == null) {
                plugin.getLogger().warning("맵 '" + mapName + "'의 영역 정보를 찾을 수 없습니다.");
                minigame.onRestorationComplete();
                return;
            }

            World world = Bukkit.getWorld(region.getWorld());
            if (world == null) {
                plugin.getLogger().warning("월드 '" + region.getWorld() + "'를 찾을 수 없습니다.");
                minigame.onRestorationComplete();
                return;
            }

            // 드롭된 아이템 제거
            removeDroppedItems(world, region);

            Set<ChunkPosition> affectedChunks = getAffectedChunks(region);

            plugin.getLogger().info("복구 시작: " + mapName + ", 영향 받은 청크 수: " + affectedChunks.size());

            restorationTask = new BukkitRunnable() {
                Iterator<ChunkPosition> chunkIterator = affectedChunks.iterator();
                boolean initialRestoreComplete = false;
                boolean needFullRestore = false;

                @Override
                public void run() {
                    long startTime = System.currentTimeMillis();
                    int chunksProcessed = 0;

                    while (chunkIterator.hasNext() && System.currentTimeMillis() - startTime < 45) {
                        ChunkPosition chunkPos = chunkIterator.next();
                        if (!initialRestoreComplete) {
                            restoreChunkFromChanges(chunkPos, chunkChanges.get(chunkPos));
                        }
                        if (needFullRestore || !initialRestoreComplete) {
                            restoreChunkFromInitialState(world, chunkPos, initialStates);
                        }
                        removeLiquidsInChunk(world, chunkPos, region, initialStates);
                        chunksProcessed++;
                    }

                    if (!chunkIterator.hasNext()) {
                        if (!initialRestoreComplete) {
                            initialRestoreComplete = true;
                            needFullRestore = !compareWithInitialState(minigame.getCOMMAND_MAIN_NAME(), mapName);
                            if (needFullRestore) {
                                plugin.getLogger().info("초기 상태와 불일치 발견. 전체 복구를 시작합니다.");
                                chunkIterator = affectedChunks.iterator();
                            } else {
                                finishRestoration(minigame, mapName);
                                this.cancel();
                            }
                        } else {
                            finishRestoration(minigame, mapName);
                            this.cancel();
                        }
                    }

                    plugin.getLogger().info("복구 진행 중: " + chunksProcessed + " 청크 처리됨");
                }
            }.runTaskTimer(plugin, 0L, 1L);
        } finally {
            restoreLock.unlock();
        }
    }

    private void removeDroppedItems(World world, CuboidRegion region) {
        BoundingBox boundingBox = BoundingBox.of(
                new Location(world, region.getMinX(), region.getMinY(), region.getMinZ()),
                new Location(world, region.getMaxX(), region.getMaxY(), region.getMaxZ())
        );

        world.getEntitiesByClass(Item.class).stream()
                .filter(item -> boundingBox.contains(item.getLocation().toVector()))
                .forEach(Entity::remove);

        plugin.getLogger().info("드롭된 아이템 제거 완료: " + region.getWorld());
    }

    private void restoreChunkFromChanges(ChunkPosition chunkPos, Set<BlockChangeRecord> changes) {
        if (changes == null) return;
        for (BlockChangeRecord change : changes) {
            change.restore(plugin);
        }
    }

    private void restoreChunkFromInitialState(World world, ChunkPosition chunkPos, Map<SerializableLocation, SerializableBlockState> initialStates) {
        int minX = chunkPos.x << 4;
        int minZ = chunkPos.z << 4;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    SerializableLocation loc = new SerializableLocation(world.getName(), x, y, z);
                    SerializableBlockState state = initialStates.get(loc);
                    if (state != null) {
                        Block block = world.getBlockAt(x, y, z);
                        if (!state.equals(new SerializableBlockState(block.getState()))) {
                            state.applyTo(block);
                        }
                    }
                }
            }
        }
    }
    private Set<ChunkPosition> getAffectedChunks(CuboidRegion region) {
        Set<ChunkPosition> chunks = new HashSet<>();
        for (int x = region.getMinX() >> 4; x <= region.getMaxX() >> 4; x++) {
            for (int z = region.getMinZ() >> 4; z <= region.getMaxZ() >> 4; z++) {
                chunks.add(new ChunkPosition(x, z));
            }
        }
        return chunks;
    }


    private void removeLiquidsInChunk(World world, ChunkPosition chunkPos, CuboidRegion region, Map<SerializableLocation, SerializableBlockState> initialStates) {
        int minX = Math.max(chunkPos.x << 4, region.getMinX());
        int maxX = Math.min((chunkPos.x << 4) + 15, region.getMaxX());
        int minZ = Math.max(chunkPos.z << 4, region.getMinZ());
        int maxZ = Math.min((chunkPos.z << 4) + 15, region.getMaxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    SerializableLocation loc = new SerializableLocation(world.getName(), x, y, z);
                    SerializableBlockState initialState = initialStates.get(loc);

                    if (LIQUID_MATERIALS.contains(block.getType())) {
                        if (initialState == null || !LIQUID_MATERIALS.contains(Material.valueOf(initialState.materialName))) {
                            // 초기 상태에 없던 액체만 제거
                            block.setType(Material.AIR, false);
                        } else {
                            // 초기 상태에 있던 액체는 원래 상태로 복원
                            initialState.applyTo(block);
                        }
                    } else if (initialState != null && LIQUID_MATERIALS.contains(Material.valueOf(initialState.materialName))) {
                        // 초기 상태에는 액체였지만 현재 액체가 아닌 경우 복원
                        initialState.applyTo(block);
                    }
                }
            }
        }
    }

    private void restoreLiquidBlocks(Map<SerializableLocation, SerializableBlockState> initialStates) {
        for (Map.Entry<SerializableLocation, SerializableBlockState> entry : initialStates.entrySet()) {
            SerializableBlockState state = entry.getValue();
            if (state.materialName.equals("WATER") || state.materialName.equals("LAVA")) {
                Location loc = entry.getKey().toBukkitLocation();
                Block block = loc.getBlock();
                state.applyTo(block);
                blocksRestored.incrementAndGet();
            }
        }
    }

    private void restoreChunk(Set<BlockChangeRecord> changes) {
        for (BlockChangeRecord change : changes) {
            change.restore(plugin);
        }
    }

    private void restoreBlock(Map.Entry<SerializableLocation, SerializableBlockState> entry) {
        Location loc = entry.getKey().toBukkitLocation();
        SerializableBlockState initialState = entry.getValue();
        Block currentBlock = loc.getBlock();

        if (!initialState.equals(new SerializableBlockState(currentBlock.getState())) &&
                !initialState.materialName.equals("AIR")) {
            initialState.applyTo(currentBlock);
        }
    }

    private int getTotalChangedBlocks(Map<ChunkPosition, Set<BlockChangeRecord>> chunkChanges) {
        return chunkChanges.values().stream().mapToInt(Set::size).sum();
    }

    private Map<Location, BlockState> loadInitialStatesForMap(String gameName, String mapName) {
        File mapFile = new File(plugin.getDataFolder(), "initial_states/" + gameName + "_" + mapName + ".dat");
        if (!mapFile.exists()) {
            return Collections.emptyMap();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(mapFile))) {
            return (Map<Location, BlockState>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "맵 '" + mapName + "'의 초기 상태 로드 중 오류 발생", e);
            return Collections.emptyMap();
        }
    }

    private boolean compareWithInitialState(String gameName, String mapName) {
        Map<SerializableLocation, SerializableBlockState> initialStates = initialBlockStates
                .getOrDefault(gameName, Collections.emptyMap())
                .getOrDefault(mapName, Collections.emptyMap());

        for (Map.Entry<SerializableLocation, SerializableBlockState> entry : initialStates.entrySet()) {
            Location loc = entry.getKey().toBukkitLocation();
            SerializableBlockState initialState = entry.getValue();
            Block currentBlock = loc.getBlock();
            if (!initialState.equals(new SerializableBlockState(currentBlock.getState()))) {
                plugin.getLogger().info("불일치 발견: " + loc + ", 초기: " + initialState + ", 현재: " + new SerializableBlockState(currentBlock.getState()));
                return false;
            }
        }
        return true;
    }
    // 복구가 종료 될 때 호출되는 메소드
    private void finishRestoration(Minigame minigame, String mapName) {
        isRestoring = false;
        if (restorationTask != null) {
            restorationTask.cancel();
            restorationTask = null;
        }
        if (worldChanges.containsKey(mapName)) {
            worldChanges.remove(mapName);
        }
        saveRestoreData();
        plugin.getLogger().info("맵 '" + mapName + "' 복구 완료. 총 " + blocksRestored.get() + "/" + totalBlocksToRestore.get() + " 블록 복구됨");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Component.text("맵 '" + mapName + "' 복구가 완료되었습니다.").color(NamedTextColor.GREEN));
        }

        if (minigame.getState() == MinigameState.IN_PROGRESS) {
            minigame.onRestorationComplete();
        }

        totalBlocksToRestore.set(0);
        blocksRestored.set(0);
    }

    public void cancelRestoration() {
        if (restorationTask != null) {
            restorationTask.cancel();
            restorationTask = null;
        }
        isRestoring = false;
        plugin.getLogger().info("복구 작업이 취소되었습니다.");
    }

    public void setRestoreRegion(String gameName, String mapName, Location pos1, Location pos2) {
        CuboidRegion region = new CuboidRegion("world",
                Math.min(pos1.getBlockX(), pos2.getBlockX()),
                Math.min(pos1.getBlockY(), pos2.getBlockY()),
                Math.min(pos1.getBlockZ(), pos2.getBlockZ()),
                Math.max(pos1.getBlockX(), pos2.getBlockX()),
                Math.max(pos1.getBlockY(), pos2.getBlockY()),
                Math.max(pos1.getBlockZ(), pos2.getBlockZ())
        );

        gameMapRegions.computeIfAbsent(gameName, k -> new ConcurrentHashMap<>())
                .put(mapName, region);

        // 맵 센터 계산 및 저장
        Location center = calculateMapCenter(gameName, mapName, pos1, pos2);
        gameMapCenters.computeIfAbsent(gameName, k -> new ConcurrentHashMap<>())
                .put(mapName, center);

        plugin.getLogger().info("복구 영역 설정: " + gameName + ", " + mapName + ", " +
                "World: world, " +
                "Min: (" + region.getMinX() + ", " + region.getMinY() + ", " + region.getMinZ() + "), " +
                "Max: (" + region.getMaxX() + ", " + region.getMaxY() + ", " + region.getMaxZ() + ")");
        plugin.getLogger().info("맵 중심 좌표 설정: " + center);
        plugin.getLogger().info("현재 gameMapRegions: " + gameMapRegions);
        plugin.getLogger().info("현재 gameMapCenters: " + gameMapCenters);
    }

    private Location calculateMapCenter(String gameName, String mapName, Location pos1, Location pos2) {
        double centerX = (pos1.getX() + pos2.getX()) / 2;
        double centerZ = (pos1.getZ() + pos2.getZ()) / 2;

        // Y 좌표 계산: 모든 스폰 위치 중 가장 높은 Y 좌표 사용
        double maxY = Math.max(pos1.getY(), pos2.getY());
        MinigameConfig config = plugin.getMinigameConfig(gameName);

        if (config != null) {
            // 모든 TeamType에 대해 스폰 위치 확인
            for (TeamType teamType : TeamType.values()) {
                List<Location> spawnLocations;
                if (teamType == TeamType.SOLO) {
                    spawnLocations = config.getSpawnLocations(mapName, teamType, 0);
                } else {
                    // 팀 게임의 경우 모든 팀의 스폰 위치 확인
                    spawnLocations = new ArrayList<>();
                    for (int i = 1; i <= 4; i++) { // 최대 4개 팀까지 가정
                        spawnLocations.addAll(config.getSpawnLocations(mapName, teamType, i));
                    }
                }

                // 레드/블루 팀 위치도 확인
                spawnLocations.addAll(config.getRedTeamLocations(mapName));
                spawnLocations.addAll(config.getBlueTeamLocations(mapName));

                // 모든 스폰 위치 중 가장 높은 Y 좌표 찾기
                for (Location loc : spawnLocations) {
                    if (loc != null && loc.getY() > maxY) {
                        maxY = loc.getY();
                    }
                }
            }
        } else {
            plugin.getLogger().warning("게임 '" + gameName + "'에 대한 설정을 찾을 수 없습니다.");
        }

        return new Location(pos1.getWorld(), centerX, maxY, centerZ);
    }

    public boolean isInRestoreRegion(String gameName, String mapName, Location location) {
        if (gameName == null || mapName == null || location == null) {
            plugin.getLogger().warning("isInRestoreRegion called with null parameter: gameName=" + gameName + ", mapName=" + mapName + ", location=" + location);
            return false;
        }

        Map<String, CuboidRegion> mapRegions = gameMapRegions.get(gameName);
        if (mapRegions == null) {
            plugin.getLogger().warning("No map regions found for game: " + gameName);
            return false;
        }

        CuboidRegion region = mapRegions.get(mapName);
        if (region == null) {
            plugin.getLogger().warning("No region found for map: " + mapName + " in game: " + gameName);
            return false;
        }

        return region.contains(location);
    }


    private void saveChunkChanges(String mapName, ChunkPosition chunkPos) {
        Set<BlockChangeRecord> changes = worldChanges.get(mapName).get(chunkPos);
        if (changes == null || changes.isEmpty()) return;

        File chunkFile = getChunkFile(mapName, chunkPos);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(chunkFile))) {
            oos.writeObject(new ArrayList<>(changes));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "청크 변경사항 저장 중 오류 발생", e);
        }
    }

    private void loadChunkChanges(String mapName, ChunkPosition chunkPos) {
        File chunkFile = getChunkFile(mapName, chunkPos);
        if (!chunkFile.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(chunkFile))) {
            List<BlockChangeRecord> changes = (List<BlockChangeRecord>) ois.readObject();
            worldChanges.computeIfAbsent(mapName, k -> new ConcurrentHashMap<>())
                    .put(chunkPos, new HashSet<>(changes));
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "청크 변경사항 로딩 중 오류 발생", e);
        }
    }

    private File getChunkFile(String mapName, ChunkPosition chunkPos) {
        File mapFolder = new File(plugin.getDataFolder(), "block_changes/" + mapName);
        if (!mapFolder.exists()) mapFolder.mkdirs();
        return new File(mapFolder, chunkPos.x + "_" + chunkPos.z + ".dat");
    }

    public void saveRestoreData() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Map<String, CuboidRegion>> gameEntry : gameMapRegions.entrySet()) {
            String gameName = gameEntry.getKey();
            for (Map.Entry<String, CuboidRegion> mapEntry : gameEntry.getValue().entrySet()) {
                String mapName = mapEntry.getKey();
                CuboidRegion region = mapEntry.getValue();
                String path = gameName + "." + mapName + ".";
                config.set(path + "world", region.getWorld());
                config.set(path + "minX", region.getMinX());
                config.set(path + "minY", region.getMinY());
                config.set(path + "minZ", region.getMinZ());
                config.set(path + "maxX", region.getMaxX());
                config.set(path + "maxY", region.getMaxY());
                config.set(path + "maxZ", region.getMaxZ());

                Location center = gameMapCenters.getOrDefault(gameName, Collections.emptyMap()).get(mapName);
                if (center != null) {
                    config.set(path + "center.x", center.getX());
                    config.set(path + "center.y", center.getY());
                    config.set(path + "center.z", center.getZ());
                    config.set(path + "center.world", center.getWorld().getName());
                }
            }
        }
        try {
            config.save(dataFile);
            plugin.getLogger().info("복구 데이터 저장 완료: " + dataFile.getAbsolutePath());
            //plugin.getLogger().info("저장된 데이터: " + config.saveToString());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "복구 데이터 저장 중 오류 발생", e);
        }
    }

    public void loadRestoreData() {
        if (!dataFile.exists()) {
            plugin.getLogger().warning("복구 데이터 파일이 존재하지 않습니다: " + dataFile.getAbsolutePath());
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        plugin.getLogger().info("로드된 데이터: " + config.saveToString());

        gameMapRegions.clear();
        gameMapCenters.clear();

        for (String gameName : config.getKeys(false)) {
            ConfigurationSection gameSection = config.getConfigurationSection(gameName);
            if (gameSection == null) {
                plugin.getLogger().warning("게임 섹션을 찾을 수 없습니다: " + gameName);
                continue;
            }
            for (String mapName : gameSection.getKeys(false)) {
                loadMapData(gameName, mapName, gameSection);
            }
        }
        plugin.getLogger().info("복구 데이터 로드 완료. gameMapRegions: " + gameMapRegions + ", gameMapCenters: " + gameMapCenters);
    }
    private void loadMapData(String gameName, String mapName, ConfigurationSection config) {
        String path = mapName + ".";
        int minX = config.getInt(path + "minX");
        int minY = config.getInt(path + "minY");
        int minZ = config.getInt(path + "minZ");
        int maxX = config.getInt(path + "maxX");
        int maxY = config.getInt(path + "maxY");
        int maxZ = config.getInt(path + "maxZ");

        CuboidRegion region = new CuboidRegion("world", minX, minY, minZ, maxX, maxY, maxZ);
        gameMapRegions.computeIfAbsent(gameName, k -> new ConcurrentHashMap<>()).put(mapName, region);
        plugin.getLogger().info("복구 영역 로드: " + gameName + ", " + mapName + ", " + region);

        if (config.contains(path + "center")) {
            double centerX = config.getDouble(path + "center.x");
            double centerY = config.getDouble(path + "center.y");
            double centerZ = config.getDouble(path + "center.z");
            Location center = new Location(Bukkit.getWorld("world"), centerX, centerY, centerZ);
            gameMapCenters.computeIfAbsent(gameName, k -> new ConcurrentHashMap<>()).put(mapName, center);
            plugin.getLogger().info("맵 중심 좌표 로드: " + gameName + ", " + mapName + ", " + center);
        }
    }

    private void scheduleDelayedLoad(String gameName, String mapName, ConfigurationSection config) {
        plugin.getLogger().info("월드가 아직 로드되지 않았습니다. 지연 로딩을 예약합니다: " + gameName + ", " + mapName);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            loadMapData(gameName, mapName, config);
        }, 100L); // 5초 후 다시 시도
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp() || event.getItem() == null || event.getItem().getType() != Material.BLAZE_ROD) return;

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            setPos1(player, event.getClickedBlock().getLocation());
            event.setCancelled(true);
        } else if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            setPos2(player, event.getClickedBlock().getLocation());
            event.setCancelled(true);
        }
    }

    private void setPos1(Player player, Location location) {
        pos1Map.put(player, location);
        player.sendMessage(ChatColor.GREEN + "복구 영역의 첫 번째 지점이 선택되었습니다: " + formatLocation(location));
    }

    private void setPos2(Player player, Location location) {
        pos2Map.put(player, location);
        player.sendMessage(ChatColor.GREEN + "복구 영역의 두 번째 지점이 선택되었습니다: " + formatLocation(location));
    }
    public void handleSetRegionCommand(Player player, String gameName, String mapName) {
        Location pos1 = pos1Map.get(player);
        Location pos2 = pos2Map.get(player);

        if (pos1 == null || pos2 == null) {
            player.sendMessage(Prefix.SERVER + "먼저 블레이즈 막대로 두 지점을 선택해주세요.");
            return;
        }

        setRestoreRegion(gameName, mapName, pos1, pos2);
        saveInitialBlockStates(gameName, mapName, pos1, pos2);
        player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'의 복구 영역이 설정되고 초기 상태가 저장되었습니다.");

        // 맵 중심 좌표 계산 및 저장
        Location center = calculateMapCenter(gameName, mapName, pos1, pos2);
        gameMapCenters.computeIfAbsent(gameName, k -> new ConcurrentHashMap<>()).put(mapName, center);
        player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'의 중심 좌표가 설정되었습니다: " + center);

        pos1Map.remove(player);
        pos2Map.remove(player);

        Map<String, CuboidRegion> mapRegions = gameMapRegions.get(gameName);
        if (mapRegions != null) {
            List<String> unsetMaps = mapRegions.keySet().stream()
                    .filter(map -> !isRestoreRegionSet(gameName, map))
                    .collect(Collectors.toList());

            if (unsetMaps.isEmpty()) {
                player.sendMessage(Prefix.SERVER + "모든 맵의 복구 영역이 설정되었습니다.");
            } else {
                player.sendMessage(Prefix.SERVER + "아직 설정되지 않은 맵: " + String.join(", ", unsetMaps));
            }
        }

        saveRestoreData();  // 변경사항 저장
    }
    private void saveInitialBlockStates(String gameName, String mapName, Location pos1, Location pos2) {
        World world = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        Map<SerializableLocation, SerializableBlockState> blockStates = new HashMap<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    blockStates.put(new SerializableLocation(loc), new SerializableBlockState(loc.getBlock().getState()));
                }
            }
        }

        initialBlockStates.computeIfAbsent(gameName, k -> new ConcurrentHashMap<>())
                .put(mapName, blockStates);

        saveInitialStates(gameName, mapName, blockStates);
        plugin.getLogger().info("초기 블록 상태 저장 완료: " + gameName + ", " + mapName);
    }

    private void saveInitialStates(String gameName, String mapName, Map<SerializableLocation, SerializableBlockState> blockStates) {
        File mapFile = new File(initialStatesFolder, gameName + "_" + mapName + ".dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(mapFile))) {
            oos.writeObject(blockStates);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "초기 블록 상태 저장 중 오류 발생: " + gameName + ", " + mapName, e);
        }
    }



    private void loadAllInitialStates() {
        File[] files = initialStatesFolder.listFiles((dir, name) -> name.endsWith(".dat"));
        if (files == null) return;

        for (File file : files) {
            String fileName = file.getName();
            String[] parts = fileName.substring(0, fileName.length() - 4).split("_");
            if (parts.length != 2) continue;

            String gameName = parts[0];
            String mapName = parts[1];

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Map<SerializableLocation, SerializableBlockState> blockStates =
                        (Map<SerializableLocation, SerializableBlockState>) ois.readObject();
                initialBlockStates.computeIfAbsent(gameName, k -> new ConcurrentHashMap<>())
                        .put(mapName, blockStates);
            } catch (IOException | ClassNotFoundException e) {
                plugin.getLogger().log(Level.SEVERE, "초기 블록 상태 로드 중 오류 발생: " + fileName, e);
            }
        }
    }


    public boolean isRestoreRegionSet(String gameName, String mapName) {
        Map<String, CuboidRegion> mapRegions = gameMapRegions.get(gameName);
        boolean isSet = mapRegions != null && mapRegions.containsKey(mapName);
        plugin.getLogger().info("isRestoreRegionSet 확인: " + gameName + ", " + mapName + " - 결과: " + isSet);
        plugin.getLogger().info("현재 gameMapRegions: " + gameMapRegions);
        return isSet;
    }
    public Location getPos1(Player player) {
        return pos1Map.get(player);
    }

    public Location getPos2(Player player) {
        return pos2Map.get(player);
    }

    public boolean isRestoring() {
        return isRestoring;
    }

    public void saveAllChanges(String mapName) {
        Map<ChunkPosition, Set<BlockChangeRecord>> changes = worldChanges.get(mapName);
        if (changes != null) {
            // 동시성 문제 해결을 위해 복사본 생성
            for (Map.Entry<ChunkPosition, Set<BlockChangeRecord>> entry : new HashMap<>(changes).entrySet()) {
                saveChunkChanges(mapName, entry.getKey());
            }
        }
        plugin.getLogger().info("모든 블록 변경사항 저장 완료: " + mapName);
    }
    public void forceRestoreAllMaps() {
        if (!restoreLock.tryLock()) {
            plugin.getLogger().warning("이미 복구 작업이 진행 중입니다.");
            return;
        }

        try {
            isRestoring = true;
            plugin.getLogger().info("모든 맵 강제 복구 시작");

            for (String mapName : new ArrayList<>(worldChanges.keySet())) {
                restoreMap(mapName);
            }

            worldChanges.clear();
            saveRestoreData();
            plugin.getLogger().info("모든 맵 강제 복구 완료");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "맵 복구 중 오류 발생", e);
        } finally {
            isRestoring = false;
            restoreLock.unlock();
        }
    }

    private void restoreMap(String mapName) {
        Map<ChunkPosition, Set<BlockChangeRecord>> changes = worldChanges.get(mapName);
        if (changes == null) return;

        int totalBlocksRestored = 0;
        for (Set<BlockChangeRecord> chunkChanges : changes.values()) {
            for (BlockChangeRecord change : chunkChanges) {
                change.restore(plugin);
                totalBlocksRestored++;
            }
        }
        plugin.getLogger().info(mapName + " 맵에서 " + totalBlocksRestored + "개의 블록 복구됨");
    }

    private String formatLocation(Location location) {
        return String.format("X: %d, Y: %d, Z: %d", location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public void reloadData() {
        loadRestoreData();
        plugin.getLogger().info("BlockRestoreManager 데이터 재로드 완료");
        plugin.getLogger().info("gameMapRegions: " + gameMapRegions);
        plugin.getLogger().info("gameMapCenters: " + gameMapCenters);
    }

    public Map<String, Map<String, CuboidRegion>> getGameMapRegions() {
        return gameMapRegions;
    }

    public Map<String, Map<String, Location>> getGameMapCenters() {
        return gameMapCenters;
    }
    private static class ChunkPosition implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int x;
        private final int z;

        public ChunkPosition(Chunk chunk) {
            this.x = chunk.getX();
            this.z = chunk.getZ();
        }

        public ChunkPosition(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkPosition that = (ChunkPosition) o;
            return x == that.x && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
    }

    private static class BlockChangeRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String worldName;
        private final int x, y, z;
        private final Material originalType;
        private final String originalBlockData;
        private final SerializableBlockState initialState;

        public BlockChangeRecord(Block block, boolean isPlaced, SerializableBlockState initialState) {
            this.worldName = block.getWorld().getName();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
            this.originalType = isPlaced ? Material.AIR : block.getType();
            this.originalBlockData = isPlaced ? Material.AIR.createBlockData().getAsString(true) : block.getBlockData().getAsString(true);
            this.initialState = initialState != null ? initialState : new SerializableBlockState(block.getState());
        }

        public void restore(EGServerMain plugin) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("월드를 찾을 수 없음: " + worldName);
                return;
            }

            Block block = world.getBlockAt(x, y, z);
            BlockState currentState = block.getState();
            if (!currentState.getType().name().equals(initialState.materialName) || !currentState.getBlockData().getAsString().equals(initialState.blockData)) {
                try {
                    initialState.applyTo(block);
                    plugin.getLogger().fine("블록 복구: " + initialState.materialName + " at X:" + x + ", Y:" + y + ", Z:" + z);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "블록 데이터 복원 실패. 기본 상태로 복원합니다.", e);
                    block.setType(Material.valueOf(initialState.materialName), false);
                }
            }
        }
    }
    private static class CuboidRegion implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String world;
        private final int minX, minY, minZ, maxX, maxY, maxZ;

        public CuboidRegion(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.world = world;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public CuboidRegion(Location pos1, Location pos2) {
            this.world = pos1.getWorld().getName();
            this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        }

        public boolean contains(Location location) {
            return location.getWorld().getName().equals(world) &&
                    location.getBlockX() >= minX && location.getBlockX() <= maxX &&
                    location.getBlockY() >= minY && location.getBlockY() <= maxY &&
                    location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
        }

        // Getter 메소드 추가
        public String getWorld() { return world; }
        public int getMinX() { return minX; }
        public int getMinY() { return minY; }
        public int getMinZ() { return minZ; }
        public int getMaxX() { return maxX; }
        public int getMaxY() { return maxY; }
        public int getMaxZ() { return maxZ; }
    }

/*
    // setRestoreRegion 메소드 수정
    public void setRestoreRegion(String gameName, String mapName, CuboidRegion region) {
        gameMapRegions.computeIfAbsent(gameName, k -> new ConcurrentHashMap<>())
                .put(mapName, region);
        saveRestoreData();
        plugin.getLogger().info("복구 영역 설정: " + gameName + ", " + mapName + ", " +
                "World: " + region.getWorld() + ", " +
                "Min: (" + region.getMinX() + ", " + region.getMinY() + ", " + region.getMinZ() + "), " +
                "Max: (" + region.getMaxX() + ", " + region.getMaxY() + ", " + region.getMaxZ() + ")");
    }

*/

    private String getCurrentGameName(Location location) {
        for (Map.Entry<String, Map<String, CuboidRegion>> gameEntry : gameMapRegions.entrySet()) {
            for (Map.Entry<String, CuboidRegion> mapEntry : gameEntry.getValue().entrySet()) {
                if (mapEntry.getValue().contains(location)) {
                    return gameEntry.getKey();
                }
            }
        }
        return null;
    }

    private String getCurrentMapName(Location location) {
        for (Map.Entry<String, Map<String, CuboidRegion>> gameEntry : gameMapRegions.entrySet()) {
            for (Map.Entry<String, CuboidRegion> mapEntry : gameEntry.getValue().entrySet()) {
                if (mapEntry.getValue().contains(location)) {
                    return mapEntry.getKey();
                }
            }
        }
        return null;
    }

    public Location getMapCenter(String gameName, String mapName) {
        plugin.getLogger().info("getMapCenter 호출: " + gameName + ", " + mapName);
        plugin.getLogger().info("현재 gameMapCenters: " + gameMapCenters);
        plugin.getLogger().info("현재 gameMapRegions: " + gameMapRegions);

        Location center = gameMapCenters.getOrDefault(gameName, Collections.emptyMap()).get(mapName);
        if (center == null) {
            plugin.getLogger().warning("맵 중심 좌표를 찾을 수 없습니다: " + gameName + ", " + mapName);
            CuboidRegion region = gameMapRegions.getOrDefault(gameName, Collections.emptyMap()).get(mapName);
            if (region != null) {
                center = calculateMapCenter(gameName, mapName,
                        new Location(Bukkit.getWorld("world"), region.getMinX(), region.getMinY(), region.getMinZ()),
                        new Location(Bukkit.getWorld("world"), region.getMaxX(), region.getMaxY(), region.getMaxZ()));
                if (center != null) {
                    gameMapCenters.computeIfAbsent(gameName, k -> new ConcurrentHashMap<>()).put(mapName, center);
                    plugin.getLogger().info("맵 중심 좌표 계산: " + gameName + ", " + mapName + ", " + center);
                    saveRestoreData();  // 새로 계산된 중심 좌표 저장
                } else {
                    plugin.getLogger().warning("맵 중심 좌표 계산 실패: " + gameName + ", " + mapName);
                }
            } else {
                plugin.getLogger().warning("맵의 영역 정보를 찾을 수 없습니다: " + gameName + ", " + mapName);
            }
        } else {
            plugin.getLogger().info("맵 중심 좌표 반환: " + gameName + ", " + mapName + ", " + center);
        }
        return center;
    }
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball)) return;

        for (Block block : event.blockList()) {
            String gameName = getCurrentGameName(block.getLocation());
            String mapName = getCurrentMapName(block.getLocation());
            if (gameName != null && mapName != null) {
                logBlockChange(block, false, gameName, mapName);
            }
        }
    }

    // SerializableLocation 클래스 수정
    private static class SerializableLocation implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String worldName;
        public final int x, y, z;

        public SerializableLocation(Location loc) {
            this.worldName = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
        }

        public SerializableLocation(String worldName, int x, int y, int z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Location toBukkitLocation() {
            World world = Bukkit.getWorld(worldName);
            return new Location(world, x, y, z);
        }

        // equals와 hashCode 메서드 구현
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SerializableLocation that = (SerializableLocation) o;
            return x == that.x && y == that.y && z == that.z && Objects.equals(worldName, that.worldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldName, x, y, z);
        }

        @Override
        public String toString() {
            return "World: " + worldName + ", X: " + x + ", Y: " + y + ", Z: " + z;
        }
    }

    // SerializableBlockState 클래스 수정
    private static class SerializableBlockState implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String materialName;
        public final String blockData;

        public SerializableBlockState(BlockState state) {
            this.materialName = state.getType().name();
            this.blockData = state.getBlockData().getAsString();
        }

        public SerializableBlockState(Material material, String blockData) {
            this.materialName = material.name();
            this.blockData = blockData;
        }

        public void applyTo(Block block) {
            Material material = Material.valueOf(materialName);
            BlockData data = Bukkit.createBlockData(blockData);
            block.setType(material);
            block.setBlockData(data);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SerializableBlockState that = (SerializableBlockState) o;
            return Objects.equals(materialName, that.materialName) && Objects.equals(blockData, that.blockData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(materialName, blockData);
        }
    }
}