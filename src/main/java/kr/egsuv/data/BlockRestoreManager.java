package kr.egsuv.data;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameState;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BlockRestoreManager implements Listener {
    private final Map<String, Map<ChunkPosition, Set<BlockChange>>> worldChanges;
    private final Map<String, CuboidRegion> restoreRegions;
    private int blocksPerTick;
    private final ReentrantLock restoreLock = new ReentrantLock();
    private final File saveFile;
    private boolean isRestoring = false;

    private final Map<Player, Location> pos1Map = new HashMap<>();
    private final Map<Player, Location> pos2Map = new HashMap<>();

    private final Map<String, Map<String, CuboidRegion>> gameMapRegions; // <GameName, <MapName, Region>>
    private static final EGServerMain plugin = EGServerMain.getInstance();

    private BukkitTask restorationTask;

    public BlockRestoreManager(int initialBlocksPerTick) {

        this.worldChanges = new ConcurrentHashMap<>();
        this.restoreRegions = new ConcurrentHashMap<>();
        this.gameMapRegions = new ConcurrentHashMap<>();
        this.blocksPerTick = initialBlocksPerTick;
        this.saveFile = new File(plugin.getDataFolder(), "block_restore_data.yml");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadRestoreData();
    }

    public void cancelRestoration() {
        if (restorationTask != null) {
            restorationTask.cancel();
            restorationTask = null;
        }
        isRestoring = false;
    }


    public void setRestoreRegion(String gameName, String mapName, Location pos1, Location pos2) {
        gameMapRegions.computeIfAbsent(gameName, k -> new ConcurrentHashMap<>())
                .put(mapName, new CuboidRegion(pos1, pos2));
        saveRestoreData();
        plugin.getLogger().info("복구 영역 설정: " + gameName + ", " + mapName + ", " + formatLocation(pos1) + " ~ " + formatLocation(pos2));
    }

    private boolean isInRestoreRegion(String gameName, String mapName, Location location) {
        Map<String, CuboidRegion> mapRegions = gameMapRegions.get(gameName);
        if (mapRegions == null) return false;
        CuboidRegion region = mapRegions.get(mapName);
        return region != null && region.contains(location);
    }

    public void logBlockChange(Block block, String gameName, String mapName) {
        if (block.getType() == Material.AIR) return;
        if (!isInRestoreRegion(gameName, mapName, block.getLocation())) {
            plugin.getLogger().info("블록이 복구 영역 밖에 있습니다: " + formatLocation(block.getLocation()));
            return;
        }

        ChunkPosition chunkPos = new ChunkPosition(block.getChunk());
        BlockChange change = new BlockChange(block);

        worldChanges.computeIfAbsent(mapName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkPos, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(change);

        plugin.getLogger().info("블록 변경 기록: " + gameName + ", " + mapName + ", " + formatLocation(block.getLocation()));

        if (worldChanges.get(mapName).get(chunkPos).size() > 1000) {
            saveChunkChanges(mapName, chunkPos);
        }
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
                minigame.finalizeGameEnd();
                return;
            }

            Map<ChunkPosition, Set<BlockChange>> chunkChanges = worldChanges.getOrDefault(mapName, new ConcurrentHashMap<>());

            plugin.getLogger().info("복구 시작: " + mapName + ", 청크 수: " + chunkChanges.size() + ", 총 변경사항: " +
                    chunkChanges.values().stream().mapToInt(Set::size).sum());

            if (chunkChanges.isEmpty()) {
                plugin.getLogger().info("복구할 블록 변경사항이 없습니다. 복구를 건너뜁니다.");
                minigame.finalizeGameEnd();
                return;
            }
            restorationTask = new BukkitRunnable() {
                Iterator<Map.Entry<ChunkPosition, Set<BlockChange>>> chunkIterator = chunkChanges.entrySet().iterator();
                Iterator<BlockChange> blockIterator = null;
                int totalRestored = 0;

                @Override
                public void run() {
                    int blocksRestored = 0;
                    long startTime = System.currentTimeMillis();

                    while (blocksRestored < blocksPerTick && System.currentTimeMillis() - startTime < 50) {
                        if (blockIterator == null || !blockIterator.hasNext()) {
                            if (!chunkIterator.hasNext()) {
                                plugin.getLogger().info("복구 완료: 총 " + totalRestored + "개 블록 복구됨");
                                finishRestoration(minigame);
                                this.cancel();
                                return;
                            }
                            Map.Entry<ChunkPosition, Set<BlockChange>> entry = chunkIterator.next();
                            if (entry != null && entry.getValue() != null) {
                                blockIterator = entry.getValue().iterator();
                                plugin.getLogger().info("새 청크 복구 시작: " + entry.getKey().x + ", " + entry.getKey().z + ", 블록 수: " + entry.getValue().size());
                            } else {
                                continue;
                            }
                        }

                        while (blockIterator.hasNext() && blocksRestored < blocksPerTick && System.currentTimeMillis() - startTime < 50) {
                            BlockChange change = blockIterator.next();
                            if (change != null) {
                                change.restore();
                                blockIterator.remove();
                                blocksRestored++;
                                totalRestored++;
                            }
                        }
                    }

                    plugin.getLogger().info("복구 진행 중: " + blocksRestored + "개 블록 복구됨 (총 " + totalRestored + ")");
                }
            }.runTaskTimer(plugin, 0L, 1L);
        } finally {
            restoreLock.unlock();
        }
    }




    public void handleBlazePodInteract(Player player, Action action) {
        if (action == Action.LEFT_CLICK_BLOCK) {
            pos1Map.put(player, player.getTargetBlock(null, 5).getLocation());
            player.sendMessage(Prefix.SERVER + "첫 번째 지점이 설정되었습니다.");
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            pos2Map.put(player, player.getTargetBlock(null, 5).getLocation());
            player.sendMessage(Prefix.SERVER + "두 번째 지점이 설정되었습니다.");
        }
    }

    //맵별 복구 영역 확인 메소드
    public boolean isRestoreRegionSet(String gameName, String mapName) {
        Map<String, CuboidRegion> mapRegions = gameMapRegions.get(gameName);
        return mapRegions != null && mapRegions.containsKey(mapName);
    }

    public Location getPos1(Player player) {
        return pos1Map.get(player);
    }

    public Location getPos2(Player player) {
        return pos2Map.get(player);
    }




    private void finishRestoration(Minigame minigame) {
        isRestoring = false;
        String mapName = minigame.getCurrentMap();
        if (mapName != null) {
            worldChanges.remove(mapName);
        }
        saveRestoreData();
        minigame.finalizeGameEnd();
    }

    private boolean isInRestoreRegion(Location location) {
        CuboidRegion region = restoreRegions.get(location.getWorld().getName());
        return region != null && region.contains(location);
    }

    private void saveChunkChanges(String worldName, ChunkPosition chunkPos) {
        Set<BlockChange> changes = worldChanges.get(worldName).get(chunkPos);
        if (changes == null || changes.isEmpty()) return;

        File chunkFile = getChunkFile(worldName, chunkPos);
        try (ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(chunkFile)))) {
            oos.writeObject(changes);
        } catch (IOException e) {
            plugin.getLogger().severe("청크 변경사항 저장 중 오류 발생: " + e.getMessage());
        }

        changes.clear();
    }

    private void loadChunkChanges(String worldName, ChunkPosition chunkPos) {
        File chunkFile = getChunkFile(worldName, chunkPos);
        if (!chunkFile.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(chunkFile)))) {
            Set<BlockChange> changes = (Set<BlockChange>) ois.readObject();
            worldChanges.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
                    .put(chunkPos, changes);
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("청크 변경사항 로딩 중 오류 발생: " + e.getMessage());
        }
    }

    private File getChunkFile(String worldName, ChunkPosition chunkPos) {
        File worldFolder = new File(plugin.getDataFolder(), "block_changes/" + worldName);
        if (!worldFolder.exists()) worldFolder.mkdirs();
        return new File(worldFolder, chunkPos.x + "_" + chunkPos.z + ".dat");
    }

    private void saveRestoreData() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Map<String, CuboidRegion>> gameEntry : gameMapRegions.entrySet()) {
            String gameName = gameEntry.getKey();
            for (Map.Entry<String, CuboidRegion> mapEntry : gameEntry.getValue().entrySet()) {
                String mapName = mapEntry.getKey();
                CuboidRegion region = mapEntry.getValue();
                config.set(gameName + "." + mapName + ".pos1", region.pos1);
                config.set(gameName + "." + mapName + ".pos2", region.pos2);
            }
        }
        try {
            config.save(saveFile);
        } catch (IOException e) {
            plugin.getLogger().severe("복구 데이터 저장 중 오류 발생: " + e.getMessage());
        }
    }

    // 명령어 처리 메소드 (별도의 CommandExecutor 클래스에서 호출)
    public void handleSetRegionCommand(Player player, String gameName, String mapName) {
        Location pos1 = pos1Map.get(player);
        Location pos2 = pos2Map.get(player);

        if (pos1 == null || pos2 == null) {
            player.sendMessage(Prefix.SERVER + "먼저 블레이즈 막대로 두 지점을 선택해주세요.");
            return;
        }

        setRestoreRegion(gameName, mapName, pos1, pos2);
        player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'의 복구 영역이 설정되었습니다.");

        // 선택된 지점 초기화
        pos1Map.remove(player);
        pos2Map.remove(player);

        // 모든 맵의 복구 영역이 설정되었는지 확인
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
    }

    public void loadRestoreData() {
        if (!saveFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(saveFile);
        for (String gameName : config.getKeys(false)) {
            ConfigurationSection gameSection = config.getConfigurationSection(gameName);
            if (gameSection == null) continue;
            for (String mapName : gameSection.getKeys(false)) {
                Location pos1 = gameSection.getLocation(mapName + ".pos1");
                Location pos2 = gameSection.getLocation(mapName + ".pos2");
                if (pos1 != null && pos2 != null) {
                    setRestoreRegion(gameName, mapName, pos1, pos2);
                }
            }
        }
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

    private String formatLocation(Location location) {
        return String.format("X: %d, Y: %d, Z: %d", location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean isRestoring() {
        return isRestoring;
    }

    public void saveAllChanges(String mapName) {
        Map<ChunkPosition, Set<BlockChange>> changes = worldChanges.get(mapName);
        if (changes != null) {
            for (Map.Entry<ChunkPosition, Set<BlockChange>> entry : changes.entrySet()) {
                saveChunkChanges(mapName, entry.getKey());
            }
        }
        plugin.getLogger().info("모든 블록 변경사항 저장 완료: " + mapName);
    }
    private static class ChunkPosition implements Serializable {
        private final int x;
        private final int z;

        public ChunkPosition(Chunk chunk) {
            this.x = chunk.getX();
            this.z = chunk.getZ();
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

    private static class BlockChange implements Serializable {
        private final String worldName;
        private final int x, y, z;
        private final Material type;
        private final byte data;

        public BlockChange(Block block) {
            this.worldName = block.getWorld().getName();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
            this.type = block.getType();
            this.data = block.getData();
        }

        @SuppressWarnings("deprecation")
        public void restore() {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Block block = world.getBlockAt(x, y, z);
                block.setType(type);
                block.setBlockData(Bukkit.createBlockData(type, (data) -> {
                    if (data instanceof Directional) {
                        ((Directional) data).setFacing(BlockFace.values()[this.data & 0x7]);
                    }
                }));
                plugin.getLogger().info("블록 복구: " + type + " at X:" + x + ", Y:" + y + ", Z:" + z);
            } else {
                plugin.getLogger().warning("월드를 찾을 수 없음: " + worldName);
            }
        }
    }

    private static class CuboidRegion implements Serializable {
        private Location pos1;
        private Location pos2;

        public CuboidRegion(Location pos1, Location pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
        }

        public void setPos1(Location pos1) {
            this.pos1 = pos1;
        }

        public void setPos2(Location pos2) {
            this.pos2 = pos2;
        }

        public boolean contains(Location location) {
            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

            return location.getBlockX() >= minX && location.getBlockX() <= maxX &&
                    location.getBlockY() >= minY && location.getBlockY() <= maxY &&
                    location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
        }
    }
    // 모든 맵 강제 복구
    public void forceRestoreAllMaps() {
        isRestoring = true;
        plugin.getLogger().info("모든 맵 강제 복구 시작");
        try {
            int totalBlocksRestored = 0;
            for (Map.Entry<String, Map<ChunkPosition, Set<BlockChange>>> worldEntry : new HashMap<>(worldChanges).entrySet()) {
                String worldName = worldEntry.getKey();
                if (worldName == null) continue;

                int worldBlocksRestored = 0;
                for (Map.Entry<ChunkPosition, Set<BlockChange>> chunkEntry : new HashMap<>(worldEntry.getValue()).entrySet()) {
                    if (chunkEntry.getKey() == null) continue;

                    for (BlockChange change : new ArrayList<>(chunkEntry.getValue())) {
                        if (change != null) {
                            change.restore();
                            worldBlocksRestored++;
                            totalBlocksRestored++;
                        }
                    }
                }
                plugin.getLogger().info(worldName + " 월드에서 " + worldBlocksRestored + "개의 블록 복구됨");
            }
            worldChanges.clear();
            saveRestoreData();
            plugin.getLogger().info("모든 맵 강제 복구 완료. 총 " + totalBlocksRestored + "개의 블록 복구됨");
        } catch (Exception e) {
            plugin.getLogger().severe("맵 복구 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isRestoring = false;
        }
    }


}