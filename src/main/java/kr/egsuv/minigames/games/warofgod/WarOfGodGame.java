package kr.egsuv.minigames.games.warofgod;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameItems;
import kr.egsuv.minigames.MinigameState;
import kr.egsuv.minigames.TeamType;
import kr.egsuv.chat.Prefix;
import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.games.warofgod.ability.*;
import kr.egsuv.minigames.gui.AbilityCheckGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.DisplaySlot;




import java.time.Duration;
import java.util.*;

public class WarOfGodGame extends Minigame implements Listener {

    private static final int BASE_ACCESS_TIME = 300; // 5분
    private static final int BASE_DISTANCE = 20;
    private static final Material CORE_BLOCK = Material.DIAMOND_BLOCK;

    private Location midChestLocation;
    private Location blueTeamChestLocation;
    private Location redTeamChestLocation;
    private Inventory midChestInventory;
    private Inventory blueTeamChestInventory;
    private Inventory redTeamChestInventory;

    private boolean baseAccessAllowed = false;

    // 현재 게임 시간
    protected int currentGameTime = 0;

    private AbilityCheckGUI abilityCheckGUI;

    private Map<Player, Ability> playerAbilities = new HashMap<>();
    private List<Class<? extends Ability>> abilityClasses = new ArrayList<>();

    public WarOfGodGame(EGServerMain plugin, MinigameItems item, String commandMainName, int MIN_PLAYER, int MAX_PLAYER, String displayGameName, boolean isTeamGame, TeamType teamType, int numberOfTeams, boolean isRedBlueTeamGame, boolean useBlockRestore) {
        super(plugin, item,  commandMainName, MIN_PLAYER, MAX_PLAYER, displayGameName, isTeamGame, teamType, numberOfTeams, isRedBlueTeamGame, useBlockRestore);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setGameRules(true, true, true, true, true);
        setGameTimeLimit(1800); // 30분

        initializeRecipes();
        // 상자 초기화
        midChestInventory = Bukkit.createInventory(null, 27, Component.text("중앙 상자"));
        blueTeamChestInventory = Bukkit.createInventory(null, 27, Component.text("블루팀 상자"));
        redTeamChestInventory = Bukkit.createInventory(null, 27, Component.text("레드팀 상자"));

        registerAbilityClasses();
        initializeChestContents();
        loadGameData();
        abilityCheckGUI = new AbilityCheckGUI(this);
    }

    private void initializeRecipes() {
        // NamespacedKey 생성
        NamespacedKey blazeRodKey = new NamespacedKey(plugin, "blaze_rod");

        // 레시피가 이미 등록되어 있는지 확인
        if (Bukkit.getRecipe(blazeRodKey) != null) {
            plugin.getLogger().warning("Recipe with key " + blazeRodKey + " already exists, skipping recipe registration.");
            return; // 이미 레시피가 있으면 등록하지 않음
        }

        // 새로운 ShapedRecipe 생성 (NamespacedKey 사용)
        ShapedRecipe blazeRodRecipe = new ShapedRecipe(blazeRodKey, new ItemStack(Material.BLAZE_ROD));

        blazeRodRecipe.shape("S", "S", "S")
                .setIngredient('S', Material.STICK);

        // 서버에 레시피 등록
        plugin.getServer().addRecipe(blazeRodRecipe);
    }

    private void registerAbilityClasses() {
        abilityClasses.add(Agnes.class);
        abilityClasses.add(Apolon.class);
        abilityClasses.add(Ares.class);
        abilityClasses.add(Artemis.class);
        abilityClasses.add(Asclypius.class);
        abilityClasses.add(Atene.class);
        abilityClasses.add(Cekmet.class);
        abilityClasses.add(Demeter.class);
        abilityClasses.add(Dionisoce.class);
        abilityClasses.add(Gaia.class);
        abilityClasses.add(Hades.class);
        abilityClasses.add(Hepaistos.class);
        abilityClasses.add(Hermes.class);
        abilityClasses.add(Nemesis.class);
        abilityClasses.add(Nicks.class);
        abilityClasses.add(Nike.class);
        abilityClasses.add(Odin.class);
        abilityClasses.add(Oneiroi.class);
        abilityClasses.add(Poseidon.class);
        abilityClasses.add(Preir.class);
        abilityClasses.add(Pulutos.class);
        abilityClasses.add(Skadi.class);
        abilityClasses.add(Tote.class);
        abilityClasses.add(Uros.class);
        abilityClasses.add(Zeus.class);
    }

    private void initializeChestContents() {
        // 중앙 상자 아이템 설정
        midChestInventory.addItem(new ItemStack(Material.IRON_INGOT, 5));
        midChestInventory.addItem(new ItemStack(Material.DIAMOND, 2));

        // 팀 상자 아이템 설정 (블루팀과 레드팀 동일)
        ItemStack[] teamChestContents = {
                new ItemStack(Material.STONE_PICKAXE, 1),
                new ItemStack(Material.STONE_SWORD, 1),
                new ItemStack(Material.COBBLESTONE, 64),
                new ItemStack(Material.COOKED_BEEF, 16)
        };
        blueTeamChestInventory.addItem(teamChestContents);
        redTeamChestInventory.addItem(teamChestContents);
    }

    @Override
    public boolean loadGameData() {
        boolean ret = true;

        // GodOfWar 게임 특정 위치 로드
        midChestLocation = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "midChest");
        blueTeamChestLocation = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "blueTeamChest");
        redTeamChestLocation = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "redTeamChest");
        Location blueCore = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "blueCore");
        Location redCore = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "redCore");
        Location blueTeamSpawnLocation = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "blueSpawn");
        Location redTeamSpawnLocation = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "redSpawn");

        if (midChestLocation == null) {
            plugin.getLogger().warning("[" + getDisplayGameName() + "] 중앙 상자 위치가 설정되지 않았습니다.");
            ret = false;
        }
        if (blueTeamChestLocation == null) {
            plugin.getLogger().warning("[" + getDisplayGameName() + "] 블루팀 상자 위치가 설정되지 않았습니다.");
            ret = false;
        }
        if (redTeamChestLocation == null) {
            plugin.getLogger().warning("[" + getDisplayGameName() + "] 레드팀 상자 위치가 설정되지 않았습니다.");
            ret = false;
        }
        if (blueCore == null) {
            plugin.getLogger().warning("[" + getDisplayGameName() + "] 블루팀 코어 위치가 설정되지 않았습니다.");
            ret = false;
        }
        if (redCore == null) {
            plugin.getLogger().warning("[" + getDisplayGameName() + "] 레드팀 코어 위치가 설정되지 않았습니다.");
            ret = false;
        }
        if (blueTeamSpawnLocation == null) {
            plugin.getLogger().warning("[" + getDisplayGameName() + "] 블루팀 스폰 중심 위치가 설정되지 않았습니다.");
            ret = false;
        }
        if (redTeamSpawnLocation == null) {
            plugin.getLogger().warning("[" + getDisplayGameName() + "] 레드팀 스폰 중심 위치가 설정되지 않았습니다.");
            ret = false;
        }

        // 상자 내용물 초기화
        initializeChestContents();

        return ret;
    }


    @Override
    protected void onGameStart() {
        baseAccessAllowed = false;
        for (Player player : getPlayers()) {
            setupPlayerForGame(player);
        }
        startGameSetTimer();
        startRound();

    }

    private void startGameSetTimer() {
        runTaskTimer(new Runnable() {
            @Override
            public void run() {
                if (getState() != MinigameState.IN_PROGRESS) {
                    cancelAllTasks();
                    return;
                }

                currentGameTime++;
                gameTimeLimit--;

                if (gameTimeLimit <= 0) {
                    endGame(false);
                } else {
                    updateScoreboard();
                    checkBaseAccess();
                }
            }
        }, 0L, 20L); // 매 초마다 실행
    }

    private void checkBaseAccess() {
        if (!baseAccessAllowed && currentGameTime >= BASE_ACCESS_TIME) {
            baseAccessAllowed = true;
            broadcastToPlayers(Component.text(Prefix.SERVER + "이제 상대방 기지에 접근할 수 있습니다!"));
        }
    }


    private void startRound() {
        broadcastTitle("§6게임 시작!", "§e신들의 전쟁이 시작되었습니다!", 10, 70, 20);
        for (Player player : getPlayers()) {
            Ability ability = playerAbilities.get(player);
            if (ability != null) {
                player.sendMessage(Component.text(Prefix.SERVER + "능력 설명:").color(NamedTextColor.GOLD));
                player.sendMessage(Component.text("주 능력: " + ability.getPrimaryDescription()).color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("보조 능력: " + ability.getSecondaryDescription()).color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("패시브: " + ability.getPassiveDescription()).color(NamedTextColor.YELLOW));
            }
        }


        updateScoreboard();
    }


    private void setupPlayerForGame(Player player) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.SURVIVAL);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        assignRandomAbility(player);
        firstGiveGameItems(player);

    }
    private void assignRandomAbility(Player player) {
        if (abilityClasses.isEmpty()) {
            player.sendMessage(Component.text(Prefix.SERVER + "사용 가능한 능력이 없습니다."));
            return;
        }

        Random random = new Random();
        Ability randomAbility = null;
        Class<? extends Ability> randomAbilityClass = null;

        // 중복되지 않은 능력을 찾을 때까지 반복
        do {
            randomAbilityClass = abilityClasses.get(random.nextInt(abilityClasses.size()));

            // 이미 할당된 능력의 클래스인지 확인
            boolean alreadyAssigned = false;
            for (Ability ability : playerAbilities.values()) {
                if (ability.getClass().equals(randomAbilityClass)) {
                    alreadyAssigned = true;
                    break;
                }
            }

            if (!alreadyAssigned) {
                try {
                    randomAbility = randomAbilityClass.getConstructor(WarOfGodGame.class, Player.class).newInstance(this, player);
                } catch (Exception e) {
                    e.printStackTrace();
                    player.sendMessage(Component.text(Prefix.SERVER + "능력을 할당하는 중 오류가 발생했습니다."));
                    return;
                }
            }
        } while (randomAbility == null);
        playerAbilities.put(player, randomAbility);
        player.sendMessage(Component.text(Prefix.SERVER + "선택된 능력: " + randomAbility.getAbilityName()));

        // Title 출력 추가
        player.showTitle(Title.title(
                Component.text(randomAbility.getAbilityName()).color(NamedTextColor.GOLD),
                Component.text("능력이 설정되었습니다.").color(NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
        ));

        if (randomAbility != null) {
            randomAbility.itemSupply();
        }

        // 패시브 스킬 활성화
        if (randomAbility != null) {
            randomAbility.passiveSkill();
        }

        plugin.getServer().getPluginManager().registerEvents(randomAbility, plugin);
        plugin.getLogger().info(player.getName() + ": " + randomAbility);
    }


    @Override
    protected void giveGameItems(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        abilityCheckGUI.giveAbilityCheckBook(player);
    }

    private void firstGiveGameItems(Player player) {
        player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
        player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
        player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET));
    }


    @Override
    protected void onGameEnd() {
        baseAccessAllowed = false;
    }

    @Override
    protected void resumeGame() {
        // 필요한 경우 게임 재개 로직 구현
    }

    @Override
    protected void setupGameSpecificRules() {
        // 게임 특정 규칙 설정
    }

    @Override
    protected void removePlayerFromScoreboard(Player player) {
        // 스코어보드에서 플레이어 제거 로직
    }

    @Override
    protected void updateScoreboard() {
        if (scoreboard == null || objective == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            objective = scoreboard.registerNewObjective("godofwar", "dummy", Component.text("§6§l신들의 전쟁").decoration(TextDecoration.BOLD, true));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int scoreValue = 15;

        // 게임 시간 표시
        int remainingMinutes = gameTimeLimit / 60;
        int remainingSeconds = gameTimeLimit % 60;
        objective.getScore("§f게임 남은 시간: §e" + String.format("%02d:%02d", remainingMinutes, remainingSeconds)).setScore(scoreValue--);

        // 상대 기지 접근 가능 시간 표시
        int accessTimeRemaining = Math.max(0, BASE_ACCESS_TIME - currentGameTime);
        int accessMinutes = accessTimeRemaining / 60;
        int accessSeconds = accessTimeRemaining % 60;
        String accessStatus = accessTimeRemaining == 0 ? "§a활성화" : String.format("§e%02d:%02d", accessMinutes, accessSeconds);
        objective.getScore("§f상대 기지 접근: " + accessStatus).setScore(scoreValue--);

        objective.getScore("§0").setScore(scoreValue--); // 빈 줄

        // 팀 정보 및 플레이어 어빌리티 표시
        for (String teamName : new String[]{"팀1", "팀2"}) {
            String teamColor = teamName.equals("팀1") ? "§9" : "§c";
            objective.getScore(teamColor + teamName + ":").setScore(scoreValue--);
            for (Player player : getPlayersTeam(teamName)) {
                Ability ability = playerAbilities.get(player);
                String abilityName = ability != null ? ability.getAbilityName() : "없음";
                objective.getScore(" §7- " + player.getName() + ": §f" + abilityName).setScore(scoreValue--);
            }
            objective.getScore("§1").setScore(scoreValue--); // 팀 사이 빈 줄
        }

        // 모든 플레이어에게 스코어보드 설정
        for (Player player : getPlayers()) {
            player.setScoreboard(scoreboard);
        }
    }


    @Override
    public void applyCustomKillStreakBonus(Player player, int streak) {
        // 킬 스트릭 보너스 적용 (필요한 경우)
    }

    @Override
    public void removeCustomKillStreakEffects(Player player) {
        // 킬 스트릭 효과 제거 (필요한 경우)
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (getState() != MinigameState.IN_PROGRESS) return;
        Player player = event.getPlayer();
        if (!getPlayers().contains(player)) return;

        Block block = event.getBlock();
        Location breakLocation = block.getLocation();

        if (breakLocation.equals(midChestLocation) || breakLocation.equals(blueTeamChestLocation) || breakLocation.equals(redTeamChestLocation)) {
            player.sendMessage(Component.text(Prefix.SERVER + "팀 상자는 파괴할 수 없습니다."));
            event.setCancelled(true);
            return;
        }

        if (block.getType() == CORE_BLOCK) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand != null && itemInHand.getType().toString().endsWith("_PICKAXE")) {
                player.sendMessage(Component.text(Prefix.SERVER + "곡괭이로는 코어 블록을 부술 수 없습니다!"));
                event.setCancelled(true);
                return;
            }
            handleCoreBreak(player, block, event);
        }
    }

    private void handleCoreBreak(Player player, Block block, BlockBreakEvent event) {
        String playerTeam = getPlayerTeam(player);
        Location blueCoreLocation = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "blueCore");
        Location redCoreLocation = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "redCore");

        if (block.getLocation().equals(blueCoreLocation)) {
            if (playerTeam.equals("팀1")) {
                player.sendMessage(Component.text(Prefix.SERVER + "자신의 팀 코어는 파괴할 수 없습니다!"));
                event.setCancelled(true);
            } else {
                endGameReason(player.getName() + "님이 블루팀의 코어를 파괴하여 레드팀이 승리했습니다!", "팀2");
            }
        } else if (block.getLocation().equals(redCoreLocation)) {
            if (playerTeam.equals("팀2")) {
                player.sendMessage(Component.text(Prefix.SERVER + "자신의 팀 코어는 파괴할 수 없습니다!"));
                event.setCancelled(true);
            } else {
                endGameReason(player.getName() + "님이 레드팀의 코어를 파괴하여 블루팀이 승리했습니다!", "팀1");
            }
        }
    }

    private String getTeamOfCore(Location location) {
        if (location.equals(getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "blueCore"))) {
            return "팀1";
        } else if (location.equals(getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "redCore"))) {
            return "팀2";
        }
        return null;
    }

    private void handleCobblestoneBreak(Player player) {
        // 코블스톤 파괴 시 추가 로직 (필요한 경우)
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (getState() != MinigameState.IN_PROGRESS) return;
        Player player = event.getPlayer();
        if (!getPlayers().contains(player)) return;

        Block block = event.getBlock();
        Location placeLocation = block.getLocation();

        if (isNearSpawn(placeLocation)) {
            player.sendMessage(Component.text(Prefix.SERVER + "스폰 지점 근처 대각선을 포함한 2칸 이내에는 블록을 설치할 수 없습니다."));
            event.setCancelled(true);
            return;
        }

        String playerTeam = getPlayerTeam(player);
        Location coreLocation = playerTeam.equals("팀1") ? getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "blueCore") : getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "redCore");
        if (isWithinDistance(placeLocation, coreLocation, 2)) {
            player.sendMessage(Component.text(Prefix.SERVER + "코어 주변 2칸 이내에는 블록을 설치할 수 없습니다."));
            event.setCancelled(true);
            return;
        }

//        super.handleBlockPlace(event.getBlockPlaced().getState());
    }

    private boolean isNearSpawn(Location location) {
        Location blueSpawn = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "blueSpawn");
        Location redSpawn = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "redSpawn");
        return (blueSpawn != null && isWithinDistance(location, blueSpawn, 2)) ||
                (redSpawn != null && isWithinDistance(location, redSpawn, 2));
    }

    private boolean isWithinDistance(Location loc1, Location loc2, int distance) {
        return Math.abs(loc1.getBlockX() - loc2.getBlockX()) <= distance &&
                Math.abs(loc1.getBlockY() - loc2.getBlockY()) <= distance &&
                Math.abs(loc1.getBlockZ() - loc2.getBlockZ()) <= distance;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!getPlayers().contains(player)) return;

        if (event.getView().getTitle().startsWith("§0자신의 능력: §c")) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null) {
                if (event.getSlot() == 11) { // "모든 신 목록" 아이콘
                    abilityCheckGUI.openAllGodsList(player);
                }
            }
        }
        abilityCheckGUI.handleInventoryClick(player, event);
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (getState() != MinigameState.IN_PROGRESS) return;
        Player player = event.getPlayer();
        if (!getPlayers().contains(player)) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null) {
                if (clickedBlock.getType() == Material.OBSIDIAN && player.getInventory().getItemInMainHand().getType() == Material.BUCKET) {
                    clickedBlock.setType(Material.AIR);
                    player.getInventory().setItemInMainHand(new ItemStack(Material.LAVA_BUCKET));
                } else if (clickedBlock.getType() == Material.CHEST) {
                    handleChestOpen(player, clickedBlock.getLocation());
                    event.setCancelled(true);
                }
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.BOOK) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§7[ §c능력 확인 §7]")) {
                    event.setCancelled(true);
                    abilityCheckGUI.openAbilityGUI(player);
                }
            }
        }

        // 능력 사용 처리
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.BLAZE_ROD) {
            Ability ability = playerAbilities.get(player);
            if (ability != null) {
                if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    if (ability.canUsePrimarySkill()) {
                        ability.primarySkill();
                    }
                } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (ability.canUseSecondarySkill()) {
                        ability.secondarySkill();
                    }
                }
            }
        }
    }

    private void handleChestOpen(Player player, Location chestLocation) {
        if (chestLocation.equals(midChestLocation)) {
            if (midChestLocation == null) {
                player.sendMessage(Component.text(Prefix.SERVER + "중앙 상자가 파괴되어 아이템 지급이 제한되었습니다."));
                return;
            }
            player.openInventory(midChestInventory);
        } else if (chestLocation.equals(blueTeamChestLocation)) {
            if (blueTeamChestLocation == null) {
                player.sendMessage(Component.text(Prefix.SERVER + "블루팀 상자가 파괴되어 아이템 지급이 제한되었습니다."));
                return;
            }
            if (getPlayerTeam(player).equals("팀1")) {
                player.openInventory(blueTeamChestInventory);
            } else {
                player.sendMessage(Component.text(Prefix.SERVER + "상대 팀의 상자는 열 수 없습니다."));
            }
        } else if (chestLocation.equals(redTeamChestLocation)) {
            if (redTeamChestLocation == null) {
                player.sendMessage(Component.text(Prefix.SERVER + "레드팀 상자가 파괴되어 아이템 지급이 제한되었습니다."));
                return;
            }
            if (getPlayerTeam(player).equals("팀2")) {
                player.openInventory(redTeamChestInventory);
            } else {
                player.sendMessage(Component.text(Prefix.SERVER + "상대 팀의 상자는 열 수 없습니다."));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (getState() != MinigameState.IN_PROGRESS) return;
        Player player = event.getPlayer();
        if (!getPlayers().contains(player)) return;

        Location to = event.getTo();
        String playerTeam = getPlayerTeam(player);
        Location enemyBase = playerTeam.equals("팀1") ? getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "redCore") : getConfig().loadLocation(getCOMMAND_MAIN_NAME(), "blueCore");

        if (!baseAccessAllowed && to.distance(enemyBase) < BASE_DISTANCE) {
            player.teleport(event.getFrom());
            player.sendMessage(Component.text(Prefix.SERVER + "아직 상대방 기지에 접근할 수 없습니다."));
        }

        if (player.getInventory().contains(Material.LAVA_BUCKET)) {
            Location ownBase = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), playerTeam.equals("팀1") ? "blueCore" : "redCore");
            if (to.distance(ownBase) > BASE_DISTANCE) {
                removeAndStoreLavaBucket(player);
            }
        }
    }

    private void removeAndStoreLavaBucket(Player player) {
        player.getInventory().remove(Material.LAVA_BUCKET);
        player.sendMessage(Component.text(Prefix.SERVER + "용암은 기지 밖 20블록까지만 가져갈 수 있습니다. 용암이 기지 창고로 이동되었습니다."));
        // 팀 창고에 용암 추가 로직 구현
        String team = getPlayerTeam(player);
        if (team.equals("팀1")) {
            blueTeamChestInventory.addItem(new ItemStack(Material.LAVA_BUCKET));
        } else {
            redTeamChestInventory.addItem(new ItemStack(Material.LAVA_BUCKET));
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;
        if (getState() != MinigameState.IN_PROGRESS) return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        if (!getPlayers().contains(damager) || !getPlayers().contains(victim)) return;

        // 이미 취소된 이벤트라면 추가 처리 하지 않음
        if (event.isCancelled()) return;

        // 능력 관련 데미지 처리
        Ability damagerAbility = playerAbilities.get(damager);
        Ability victimAbility = playerAbilities.get(victim);
        if (damagerAbility != null) {
            damagerAbility.onDamageDealt(event);
        }
        if (victimAbility != null) {
            victimAbility.onDamageReceived(event);
        }
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!getPlayers().contains(victim)) return;

        Player killer = victim.getKiller();
        if (killer != null && getPlayers().contains(killer)) {
            handleKill(killer, victim);
        }

        // 죽었을 때 드롭되는 아이템 목록에서 [ 능력 확인 ] 이름의 책을 제거
        event.getDrops().removeIf(item -> {
            if (item.getType() == Material.BOOK) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() &&
                        PlainTextComponentSerializer.plainText().serialize(meta.displayName()).equals("§7[ §c능력 확인 §7]")) {
                    return true; // 드롭 목록에서 제거
                }
            }
            return false;
        });

        handlePlayerDeath(victim);
    }

    private void handleKill(Player killer, Player victim) {
        Ability killerAbility = playerAbilities.get(killer);
        if (killerAbility != null) {
            killerAbility.onKill(victim);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!getPlayers().contains(player)) return;

        // 배고픔 감소 로직 구현
        // 예: 일반적인 속도보다 느리게 감소
        if (event.getFoodLevel() < player.getFoodLevel()) {
            event.setFoodLevel(player.getFoodLevel() - 1);
        }
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (!getPlayers().contains(player) || getState() != MinigameState.IN_PROGRESS) return;

        // 플레이어가 양동이로 무엇을 붓는지 확인
        if (event.getBucket() == Material.LAVA_BUCKET) {
            Location placeLoc = event.getBlock().getLocation();
            Location baseLocation = getConfig().loadLocation(getCOMMAND_MAIN_NAME(), getPlayerTeam(player).equals("팀1") ? "blueCore" : "redCore");

            // 기지에서 일정 거리 이상 떨어진 곳에 용암을 설치할 수 없도록 제한
            if (placeLoc.distance(baseLocation) > BASE_DISTANCE) {
                event.setCancelled(true);
                player.sendMessage(Component.text(Prefix.SERVER + "기지에서 20블록 이상 떨어진 곳에는 용암을 설치할 수 없습니다."));
            }
        }
    }

    @Override
    public void showRules(Player player) {
        player.sendMessage(Component.text("=== 신들의 전쟁 규칙 ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("1. 각 팀은 자신의 코어를 보호하고 상대팀의 코어를 파괴해야 합니다.").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("2. 코블스톤을 모아 능력을 사용하고 상대방 진영으로 이동하세요.").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("3. 게임 시작 후 5분간은 상대 기지에 접근할 수 없습니다.").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("4. 용암은 자신의 기지에서 20블록 이상 떨어진 곳에 설치할 수 없습니다.").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("5. 팀원을 공격할 수 없으며, 사망 시 3초 후 리스폰됩니다.").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("6. 블레이즈 막대로 능력을 사용할 수 있습니다. (좌클릭/우클릭)").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("7. 게임 시간은 30분이며, 시간 내 코어를 파괴하지 못하면 무승부로 끝납니다.").color(NamedTextColor.WHITE));
    }

    private void endGameReason(String reason, String winningTeam) {
        cancelAllTasks(); // 모든 진행 중인 작업 취소
        // 모든 능력에 대한 이벤트 리스너 등록
        for (Ability ability : playerAbilities.values()) {
            plugin.getServer().getPluginManager().registerEvents(ability, plugin);
        }
        for (Player player : getPlayers()) {

            Ability playerAbility = getPlayerAbility(player);
            playerAbility.initCoolDowns();

            player.sendMessage(Component.text(Prefix.SERVER + reason).color(NamedTextColor.GOLD));
            if (winningTeam != null) {
                if (getPlayerTeam(player).equals(winningTeam)) {
                    player.showTitle(Title.title(
                            Component.text("승리!").color(NamedTextColor.GOLD),
                            Component.text("당신의 팀이 승리했습니다!").color(NamedTextColor.YELLOW),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(500))
                    ));
                } else {
                    player.showTitle(Title.title(
                            Component.text("패배").color(NamedTextColor.RED),
                            Component.text("아쉽게도 패배했습니다.").color(NamedTextColor.YELLOW),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(500))
                    ));
                }
            } else {
                player.showTitle(Title.title(
                        Component.text("무승부").color(NamedTextColor.YELLOW),
                        Component.text("게임이 종료되었습니다.").color(NamedTextColor.WHITE),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(500))
                ));
            }
        }
        runTaskLater(() -> super.endGame(false), 100L);
    }

    @Override
    protected void resetGameSpecificData() {
        baseAccessAllowed = false;
        playerAbilities.clear();
        // 추가적인 게임 특정 데이터 초기화
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!players.contains(player)) return;

        String[] args = event.getMessage().split(" ");
        if (args[0].equalsIgnoreCase("/" + getCOMMAND_MAIN_NAME())) {
            if (args.length > 1) {
                switch (args[1].toLowerCase()) {
                    case "setmidchest":
                        if (player.isOp()) {
                            setMidChestLocation(player);
                            player.sendMessage(Component.text(Prefix.SERVER + "중앙 상자 위치가 설정되었습니다."));
                        }
                        break;
                    case "setbluechest": //팀 1
                        if (player.isOp()) {
                            setBlueTeamChestLocation(player);
                            player.sendMessage(Component.text(Prefix.SERVER + "블루팀 상자 위치가 설정되었습니다."));
                        }
                        break;
                    case "setredchest": //팀 2
                        if (player.isOp()) {
                            setRedTeamChestLocation(player);
                            player.sendMessage(Component.text(Prefix.SERVER + "레드팀 상자 위치가 설정되었습니다."));
                        }
                        break;
                    case "setbluecore":
                        if (player.isOp()) {
                            setBlueCoreLocation(player);
                            player.sendMessage(Component.text(Prefix.SERVER + "블루팀 코어 위치가 설정되었습니다."));
                        }
                        break;
                    case "setredcore":
                        if (player.isOp()) {
                            setRedCoreLocation(player);
                            player.sendMessage(Component.text(Prefix.SERVER + "레드팀 코어 위치가 설정되었습니다."));
                        }
                        break;
                    case "setbluespawn":
                        if (player.isOp()) {
                            setBlueSpawnLocation(player);
                            player.sendMessage(Component.text(Prefix.SERVER + "블루팀 스폰 중심 위치가 설정되었습니다."));
                        }
                        break;
                    case "setredspawn": //갔다오면 load, 변수 넣기
                        if (player.isOp()) {
                            setRedSpawnLocation(player);
                            player.sendMessage(Component.text(Prefix.SERVER + "레드팀 스폰 중심 위치가 설정되었습니다."));
                        }
                        break;
                    case "능력":
                        if (players.contains(player)) {
                            Ability playerAbility = getPlayerAbility(player);
                            if (playerAbility != null) {
                                playerAbility.showHelp();
                            } else {
                                player.sendMessage(Component.text(Prefix.SERVER + "능력이 설정되지 않았습니다."));
                            }
                        } else {
                            player.sendMessage(Component.text(Prefix.SERVER + "게임에 참여 중이지 않습니다."));
                        }
                        break;
                }
            }
            event.setCancelled(true);
        }
    }

    private Ability getPlayerAbility(Player player) {
        return playerAbilities.get(player);
    }


    // 위치 저장 메소드들
    private void setMidChestLocation(Player player) {
        Location location = getPlayerTargetBlockLocation(player, 50);
        if (location != null) {
            midChestLocation = location;
            config.saveLocation(getCOMMAND_MAIN_NAME(), "midChest", location);
            player.sendMessage(Component.text(Prefix.SERVER + "중앙 상자 위치가 설정되었습니다."));
        } else {
            player.sendMessage(Component.text(Prefix.SERVER + "유효한 블록을 바라보고 있지 않습니다."));
        }
    }

    private void setBlueTeamChestLocation(Player player) {
        Location location = getPlayerTargetBlockLocation(player, 50);
        if (location != null) {
            blueTeamChestLocation = location;
            config.saveLocation(getCOMMAND_MAIN_NAME(), "blueTeamChest", location);
            player.sendMessage(Component.text(Prefix.SERVER + "블루팀 상자 위치가 설정되었습니다."));
        } else {
            player.sendMessage(Component.text(Prefix.SERVER + "유효한 블록을 바라보고 있지 않습니다."));
        }
    }

    private void setRedTeamChestLocation(Player player) {
        Location location = getPlayerTargetBlockLocation(player, 50);
        if (location != null) {
            redTeamChestLocation = location;
            config.saveLocation(getCOMMAND_MAIN_NAME(), "redTeamChest", location);
            player.sendMessage(Component.text(Prefix.SERVER + "레드팀 상자 위치가 설정되었습니다."));
        } else {
            player.sendMessage(Component.text(Prefix.SERVER + "유효한 블록을 바라보고 있지 않습니다."));
        }
    }

    private void setBlueCoreLocation(Player player) {
        Location location = getPlayerTargetBlockLocation(player, 50);
        if (location != null) {
            config.saveLocation(getCOMMAND_MAIN_NAME(), "blueCore", location);
            player.sendMessage(Component.text(Prefix.SERVER + "블루팀 코어 위치가 설정되었습니다."));
        } else {
            player.sendMessage(Component.text(Prefix.SERVER + "유효한 블록을 바라보고 있지 않습니다."));
        }
    }

    private void setRedCoreLocation(Player player) {
        Location location = getPlayerTargetBlockLocation(player, 50);
        if (location != null) {
            config.saveLocation(getCOMMAND_MAIN_NAME(), "redCore", location);
            player.sendMessage(Component.text(Prefix.SERVER + "레드팀 코어 위치가 설정되었습니다."));
        } else {
            player.sendMessage(Component.text(Prefix.SERVER + "유효한 블록을 바라보고 있지 않습니다."));
        }
    }


    private void setBlueSpawnLocation(Player player) {
        Location location = getPlayerTargetBlockLocation(player, 50);
        if (location != null) {
            config.saveLocation(getCOMMAND_MAIN_NAME(), "blueSpawn", location);
            player.sendMessage(Component.text(Prefix.SERVER + "블루팀 스폰 기준 위치가 설정되었습니다."));
        } else {
            player.sendMessage(Component.text(Prefix.SERVER + "유효한 블록을 바라보고 있지 않습니다."));
        }
    }

    private void setRedSpawnLocation(Player player) {
        Location location = getPlayerTargetBlockLocation(player, 50);
        if (location != null) {
            config.saveLocation(getCOMMAND_MAIN_NAME(), "redSpawn", location);
            player.sendMessage(Component.text(Prefix.SERVER + "레드팀 스폰 기준 위치가 설정되었습니다."));
        } else {
            player.sendMessage(Component.text(Prefix.SERVER + "유효한 블록을 바라보고 있지 않습니다."));
        }
    }



    // 플레이어가 바라보고 있는 블록의 위치를 반환하는 메소드
    private Location getPlayerTargetBlockLocation(Player player, int maxDistance) {
        Block targetBlock = player.getTargetBlockExact(maxDistance);

        if (targetBlock == null) {
            return null; // 유효한 블록이 없는 경우 null 반환
        }

        return targetBlock.getLocation();
    }

    public Map<Player, Ability> getPlayerAbilities() {
        return playerAbilities;
    }

    public List<Class<? extends Ability>> getAbilityClasses() {
        return abilityClasses;
    }
}