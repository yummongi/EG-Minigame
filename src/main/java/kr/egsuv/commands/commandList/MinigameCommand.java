package kr.egsuv.commands.commandList;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.Command;
import kr.egsuv.config.MinigameConfig;
import kr.egsuv.data.BlockRestoreManager;
import kr.egsuv.data.MinigameData;
import kr.egsuv.data.PlayerData;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigamePenaltyManager;
import kr.egsuv.minigames.MinigameState;
import kr.egsuv.minigames.TeamType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MinigameCommand implements Command {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private static final String PERMISSION_ADMIN = "minigame.admin";
    private static final String USAGE_PREFIX = "§7[§c!§7] §f사용법: /minigame §7";

    @Override
    public boolean executeCommand(Player player, String[] args) {
        if (args.length == 0) {
            sendGeneralHelpMessage(player);
            return true;
        }

        String minigameName = args[0].toLowerCase();
        Minigame game = plugin.getMinigameByName(minigameName);

        if (game == null) {
            player.sendMessage(Prefix.SERVER + "해당 미니게임을 찾을 수 없습니다.");
            return true;
        }

        if (args.length == 1) {
            sendHelpMessage(player, game, false);
            return true;
        }

        String subCommand = args[1].toLowerCase();
        return executeSubCommand(player, game, subCommand, args);
    }

    private boolean executeSubCommand(Player player, Minigame game, String subCommand, String[] args) {
        switch (subCommand) {
            case "join":
                return joinGame(player, game);
            case "quit":
                return quitGame(player, game);
            case "start":
                return startGame(player, game);
            case "stop":
                return stopGame(player, game);
            case "set":
                return handleSetCommand(player, game, args);
            case "restore":
                return handleRestoreCommand(player, game, args);
            case "remove":
                return handleRemoveCommand(player, game, args);
            case "list":
                return listSpawnLocations(player, game);
            case "map":
                return listMaps(player, game);
            case "info":
                return showGameInfo(player, game);
            case "mapinfo":
                return showMapInfo(player, game, args);
            case "rules":
                return showGameRules(player, game);
            case "top":
                return showTopPlayers(player, game);
            case "stats":
                return showPlayerStats(player, game, args);
            case "teamchat":
            case "tc":
                return toggleTeamChat(player, game);
            case "reload":
                return handleReloadConfig(player, game);
            default:
                sendHelpMessage(player, game, true);
                return true;
        }
    }


    private boolean handleRestoreCommand(Player player, Minigame game, String[] args) {
        if (!game.useBlockRestore) {
            player.sendMessage(Prefix.SERVER + "이 게임은 블록 복구 기능을 사용하지 않습니다.");
            return true;
        }

        if (args.length < 4) {
            sendRestoreHelpMessage(player, game);
            return true;
        }

        String restoreAction = args[2].toLowerCase();
        String mapName = args[3];

        switch (restoreAction) {
            case "setregion":
                return handleSetRegionCommand(player, game, args);
            case "start":
                return handleStartRestoreCommand(player, game, mapName);
            case "cancel":
                return handleCancelRestoreCommand(player, game, mapName);
            default:
                sendRestoreHelpMessage(player, game);
                return true;
        }
    }


    private boolean handleSetRegionCommand(Player player, Minigame game, String[] args) {
        if (args.length < 4) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " restore setregion <맵이름>");
            return true;
        }

        String mapName = args[3];
        if (!game.getConfig().getMaps().contains(mapName)) {
            player.sendMessage(Prefix.SERVER + "존재하지 않는 맵입니다: " + mapName);
            return true;
        }

        if (!game.useBlockRestore) {
            player.sendMessage(Prefix.SERVER + "이 게임은 블록 복구 기능을 사용하지 않습니다.");
            return true;
        }

        game.getBlockRestoreManager().handleSetRegionCommand(player, game.getCOMMAND_MAIN_NAME(), mapName);
        return true;
    }

    private boolean handleStartRestoreCommand(Player player, Minigame game, String mapName) {
        if (game.getState() != MinigameState.WAITING) {
            player.sendMessage(Prefix.SERVER + "게임이 대기 상태일 때만 복구를 시작할 수 있습니다.");
            return true;
        }

        if (!game.getConfig().getMaps().contains(mapName)) {
            player.sendMessage(Prefix.SERVER + "존재하지 않는 맵입니다: " + mapName);
            return true;
        }

        game.setState(MinigameState.REPAIRING);
        game.getBlockRestoreManager().startRestoration(game, mapName);
        player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "' 복구를 시작합니다.");
        return true;
    }

    private boolean handleCancelRestoreCommand(Player player, Minigame game, String mapName) {
        if (game.getState() != MinigameState.REPAIRING) {
            player.sendMessage(Prefix.SERVER + "현재 복구 중이 아닙니다.");
            return true;
        }

        if (!game.getCurrentMap().equals(mapName)) {
            player.sendMessage(Prefix.SERVER + "현재 복구 중인 맵이 아닙니다: " + mapName);
            return true;
        }

        game.getBlockRestoreManager().cancelRestoration();
        game.setState(MinigameState.WAITING);
        player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "' 복구가 취소되었습니다.");
        return true;
    }

    private boolean handleReloadConfig(Player player, Minigame game) {
        if (!hasAdminPermission(player)) return true;
        game.getConfig().reloadConfig();
        player.sendMessage(Prefix.SERVER + game.getDisplayGameName() + "의 설정이 다시 로드되었습니다.");
        return true;
    }

    private boolean toggleTeamChat(Player player, Minigame game) {
        if (!game.isTeamGame() || !game.getPlayers().contains(player)) {
            player.sendMessage(Prefix.SERVER + "팀 채팅은 팀 게임 중에만 사용할 수 있습니다.");
            return true;
        }

        game.toggleTeamChat(player);
        return true;
    }

    private boolean joinGame(Player player, Minigame game) {
        game.joinGame(player);
        return true;
    }

    private boolean quitGame(Player player, Minigame game) {
        game.handleCommandQuit(player);
        return true;
    }

    private boolean startGame(Player player, Minigame game) {
        if (!hasAdminPermission(player)) return true;
        game.forceStart();
        return true;
    }

    private boolean stopGame(Player player, Minigame game) {
        if (!hasAdminPermission(player)) return true;
        game.forceEndGame();
        player.sendMessage("§c15초 뒤 게임이 종료됩니다. 반드시 명령어를 한번만 치세요");
        return true;
    }

    private boolean check(Player player, Minigame game) {
        if (!hasAdminPermission(player)) return true;
        if (game.getState() == MinigameState.DISABLED) {
            game.setState(MinigameState.WAITING);
            player.sendMessage("미니게임을 활성화 하였습니다.");
        } else if (game.getState() == MinigameState.WAITING) {
            game.setState(MinigameState.DISABLED);
            player.sendMessage("미니게임을 비활성화 하였습니다.");
        }
        return true;
    }

    private boolean handleSetCommand(Player player, Minigame game, String[] args) {
        if (!hasAdminPermission(player)) return true;
        if (args.length < 3) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set <lobby|spawn|map|mapicon|min|max|time> [추가 인자]");
            return true;
        }

        String type = args[2].toLowerCase();
        Location playerLocation = player.getLocation();

        switch (type) {
            case "lobby":
                game.getConfig().setGameLobby(playerLocation);
                player.sendMessage(Prefix.SERVER + "게임 로비 위치가 설정되었습니다.");
                break;
            case "spawn":
                if (args.length < 5) {
                    player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set spawn <맵이름> <팀타입/팀이름> [팀번호]");
                    return true;
                }
                return handleSetSpawnCommand(player, game, args);
            case "map":
                if (args.length < 4) {
                    player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set map <맵이름>");
                    return true;
                }
                return handleSetMapCommand(player, game, args);
            case "mapicon":
                if (args.length < 4) {
                    player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set mapicon <맵이름>");
                    return true;
                }
                return handleSetMapIcon(player, game, args);
            case "min":
            case "max":
            case "time":
                if (args.length < 4) {
                    player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set " + type + " <숫자>");
                    return true;
                }
                return handleSetGameSettings(player, game, type, args);
            default:
                player.sendMessage(Prefix.SERVER + "알 수 없는 설정 유형입니다. lobby, spawn, map, mapicon, min, max 또는 time을 사용하세요.");
                break;
        }
        return true;
    }

    private boolean handleSetMapCommand(Player player, Minigame game, String[] args) {
        if (args.length < 4) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set map <맵이름>");
            return true;
        }
        String mapName = args[3];
        game.getConfig().addMap(mapName);
        player.sendMessage(Prefix.SERVER + "새 맵 '" + mapName + "'이(가) 추가되었습니다.");
        return true;
    }

    private boolean handleSetMapIcon(Player player, Minigame game, String[] args) {
        if (args.length < 4) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set mapicon <맵이름>");
            return true;
        }
        String mapName = args[3];
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(Prefix.SERVER + "손에 아이템을 들고 명령어를 사용해주세요.");
            return true;
        }
        game.getConfig().setMapIcon(mapName, itemInHand.clone());
        player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'의 아이콘이 설정되었습니다.");
        return true;
    }

    private boolean handleSetSpawnCommand(Player player, Minigame game, String[] args) {
        if (args.length < 5) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set spawn <mapName> <teamType/teamName> [teamNumber]");
            return true;
        }
        String mapName = args[3];
        String teamTypeOrName = args[4].toUpperCase();
        Location playerLocation = player.getLocation();

        if (teamTypeOrName.equals("RED") || teamTypeOrName.equals("BLUE")) {
            game.getConfig().addTeamLocation(mapName, teamTypeOrName.toLowerCase(), playerLocation);
            player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'의 " + teamTypeOrName + " 팀 스폰 위치가 추가되었습니다.");
        } else {
            TeamType teamType;
            try {
                teamType = TeamType.valueOf(teamTypeOrName);
            } catch (IllegalArgumentException e) {
                player.sendMessage(Prefix.SERVER + "올바른 팀 타입을 입력해주세요 (SOLO, DUO, TRIPLE, SQUAD).");
                return true;
            }
            int teamNumber = 0;
            if (teamType != TeamType.SOLO) {
                if (args.length < 6) {
                    player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set spawn <mapName> <teamType> <teamNumber>");
                    return true;
                }
                try {
                    teamNumber = Integer.parseInt(args[5]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Prefix.SERVER + "팀 번호는 숫자여야 합니다.");
                    return true;
                }
            }
            game.getConfig().addSpawnLocation(mapName, teamType, teamNumber, playerLocation);
            player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'의 " + teamType + (teamType != TeamType.SOLO ? " " + teamNumber + "팀" : "") + " 스폰 위치가 추가되었습니다.");
        }
        return true;
    }

    private void setTeamType(Player player, Minigame game, String[] args) {
        if (args.length < 5) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set spawn teamtype <solo|duo|triple|squad>");
            return;
        }
        try {
            TeamType teamType = TeamType.valueOf(args[4].toUpperCase());
            game.getConfig().setTeamType(teamType);
            player.sendMessage(Prefix.SERVER + "팀 타입이 " + teamType + "로 설정되었습니다.");
        } catch (IllegalArgumentException e) {
            player.sendMessage(Prefix.SERVER + "올바른 팀 타입을 입력해주세요 (solo, duo, triple, squad).");
        }
    }

    private boolean handleRemoveCommand(Player player, Minigame game, String[] args) {
        if (!hasAdminPermission(player)) return true;
        if (args.length < 4) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " remove <spawn|lobby|map> <mapName> [teamType/teamName] [teamNumber]");
            return true;
        }

        String type = args[2].toLowerCase();
        String mapName = args[3];
        MinigameConfig config = game.getConfig();

        switch (type) {
            case "spawn":
                if (args.length < 5) {
                    player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " remove spawn <mapName> <teamType/teamName> [teamNumber]");
                    return true;
                }
                String teamTypeOrName = args[4].toUpperCase();
                if (teamTypeOrName.equals("RED") || teamTypeOrName.equals("BLUE")) {
                    List<Location> locations = config.getTeamLocations(mapName, teamTypeOrName.toLowerCase());
                    if (locations.isEmpty()) {
                        player.sendMessage(Prefix.SERVER + "해당 팀의 스폰 위치가 존재하지 않습니다.");
                        return true;
                    }
                    config.removeTeamSpawnLocation(mapName, teamTypeOrName.toLowerCase());
                    player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'의 " + teamTypeOrName + " 팀 스폰 위치가 제거되었습니다.");
                } else {
                    TeamType teamType;
                    try {
                        teamType = TeamType.valueOf(teamTypeOrName);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Prefix.SERVER + "올바른 팀 타입을 입력해주세요 (SOLO, DUO, TRIPLE, SQUAD).");
                        return true;
                    }
                    int teamNumber = 0;
                    if (teamType != TeamType.SOLO) {
                        if (args.length < 6) {
                            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " remove spawn <mapName> <teamType> <teamNumber>");
                            return true;
                        }
                        try {
                            teamNumber = Integer.parseInt(args[5]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(Prefix.SERVER + "팀 번호는 숫자여야 합니다.");
                            return true;
                        }
                    }
                    removeSpawnLocation(player, game, mapName, teamType, teamNumber);
                }
                break;
            case "lobby":
                removeLobby(player, game, mapName);
                break;
            case "map":
                removeMap(player, game, mapName);
                break;
            default:
                player.sendMessage(Prefix.SERVER + "알 수 없는 제거 유형입니다. spawn, lobby 또는 map을 사용하세요.");
                break;
        }
        return true;
    }

    private void removeSpawnLocation(Player player, Minigame game, String mapName, TeamType teamType, int teamNumber) {
        MinigameConfig config = game.getConfig();
        List<Location> locations = config.getSpawnLocations(mapName, teamType, teamNumber);
        if (locations.isEmpty()) {
            player.sendMessage(Prefix.SERVER + "해당 스폰 위치가 존재하지 않습니다.");
            return;
        }
        config.removeSpawnLocation(mapName, teamType, teamNumber);
        player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'의 " + teamType + " " + teamNumber + "팀 스폰 위치가 제거되었습니다.");
    }

    private void removeLobby(Player player, Minigame game, String mapName) {
        MinigameConfig config = game.getConfig();
        if (config.getGameLobby() == null) {
            player.sendMessage(Prefix.SERVER + "해당 맵의 로비가 설정되어 있지 않습니다.");
            return;
        }
        config.removeGameLobby();
        player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'의 로비가 제거되었습니다.");
    }

    private void removeMap(Player player, Minigame game, String mapName) {
        MinigameConfig config = game.getConfig();
        if (!config.getMaps().contains(mapName)) {
            player.sendMessage(Prefix.SERVER + "해당 맵이 존재하지 않습니다.");
            return;
        }
        config.removeMap(mapName);
        player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'이(가) 제거되었습니다.");
    }

    private boolean listSpawnLocations(Player player, Minigame game) {
        for (String mapName : game.getConfig().getMaps()) {
            player.sendMessage(Prefix.SERVER + "맵 '" + mapName + "'의 스폰 위치:");
            for (TeamType teamType : TeamType.values()) {
                if (teamType == TeamType.SOLO) {
                    List<Location> locations = game.getConfig().getSpawnLocations(mapName, teamType, 0);
                    for (int i = 0; i < locations.size(); i++) {
                        Location loc = locations.get(i);
                        player.sendMessage(String.format("%s %d: %s, %.2f, %.2f, %.2f",
                                teamType.name(), i, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
                    }
                } else {
                    for (int teamNumber = 1; teamNumber <= teamType.getPlayersPerTeam(); teamNumber++) {
                        List<Location> locations = game.getConfig().getSpawnLocations(mapName, teamType, teamNumber);
                        for (int i = 0; i < locations.size(); i++) {
                            Location loc = locations.get(i);
                            player.sendMessage(String.format("%s 팀 %d, 위치 %d: %s, %.2f, %.2f, %.2f",
                                    teamType.name(), teamNumber, i, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
                        }
                    }
                }
            }
            // RED와 BLUE 팀 스폰 위치 표시
            for (String teamName : new String[]{"red", "blue"}) {
                List<Location> locations = game.getConfig().getTeamLocations(mapName, teamName);
                for (int i = 0; i < locations.size(); i++) {
                    Location loc = locations.get(i);
                    player.sendMessage(String.format("%s 팀, 위치 %d: %s, %.2f, %.2f, %.2f",
                            teamName.toUpperCase(), i, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
                }
            }
        }
        return true;
    }


    private boolean listMaps(Player player, Minigame game) {
        List<String> maps = game.getConfig().getMaps();
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 맵 목록 ===");
        for (String map : maps) {
            player.sendMessage(Prefix.SERVER + "- " + map);
        }
        return true;
    }

    private boolean showMapInfo(Player player, Minigame game, String[] args) {
        if (args.length < 3) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " mapinfo <맵이름>");
            return true;
        }
        String mapName = args[2];
        MinigameConfig config = game.getConfig();

        player.sendMessage(Prefix.SERVER + "=== 맵 '" + mapName + "' 정보 ===");
        Location lobby = config.getGameLobby();
        player.sendMessage(Prefix.SERVER + "로비 위치: " + (lobby != null ? formatLocation(lobby) : "설정되지 않음"));

        listRedBlueTeamSpawnLocations(player, config, mapName, "레드 팀", "red");
        listRedBlueTeamSpawnLocations(player, config, mapName, "블루 팀", "blue");

        List<TeamType> teamTypes = Arrays.asList(TeamType.SOLO, TeamType.DUO, TeamType.TRIPLE, TeamType.SQUAD);
        for (TeamType teamType : teamTypes) {
            List<Location> spawns = config.getSpawnLocations(mapName, teamType, 0);
            player.sendMessage(Prefix.SERVER + teamType.name() + " 스폰 위치:");
            if (spawns.isEmpty()) {
                player.sendMessage(Prefix.SERVER + "  설정되지 않음");
            } else {
                for (int i = 0; i < spawns.size(); i++) {
                    player.sendMessage(Prefix.SERVER + "  " + (i + 1) + ": " + formatLocation(spawns.get(i)));
                }
            }
        }
        return true;
    }
    private void listRedBlueTeamSpawnLocations(Player player, MinigameConfig config, String mapName, String teamDisplayName, String teamName) {
        List<Location> teamSpawn = config.getTeamSpawnLocations(mapName, teamName);
        player.sendMessage(Prefix.SERVER + teamDisplayName + " 스폰 위치:");
        if (teamSpawn.isEmpty()) {
            player.sendMessage(Prefix.SERVER + "  설정되지 않음");
        } else {
            for (int i = 0; i < teamSpawn.size(); i++) {
                player.sendMessage(Prefix.SERVER + "  " + (i + 1) + ": " + formatLocation(teamSpawn.get(i)));
            }
        }
    }

    private String formatLocation(Location loc) {
        return String.format("%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ());
    }

    private boolean showGameInfo(Player player, Minigame game) {
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 정보 ===");
        player.sendMessage(Prefix.SERVER + "현재 상태: " + game.getState());
        player.sendMessage(Prefix.SERVER + "참가자 수: " + game.getPlayers().size() + "/" + game.getMAX_PLAYER());
        player.sendMessage(Prefix.SERVER + "최소 시작 인원: " + game.getMIN_PLAYER());
        player.sendMessage(Prefix.SERVER + "게임 시간 제한: " + game.getGameTimeLimit() + "초");
        return true;
    }

    private boolean showGameRules(Player player, Minigame game) {
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 규칙 ===");
        game.showRules(player);
        return true;
    }

    private boolean showTopPlayers(Player player, Minigame game) {
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 상위 플레이어 ===");
        List<Map.Entry<String, Double>> topPlayers = plugin.getRankingManager().getTopPlayers(game.getCOMMAND_MAIN_NAME(), 10);
        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<String, Double> entry = topPlayers.get(i);
            player.sendMessage(String.format("%d. %s - %.2f점", i + 1, entry.getKey(), entry.getValue()));
        }
        return true;
    }

    private boolean showPlayerStats(Player player, Minigame game, String[] args) {
        Player target = player;
        if (args.length > 2) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                player.sendMessage(Prefix.SERVER + "해당 플레이어를 찾을 수 없습니다.");
                return true;
            }
        }

        PlayerData playerData = plugin.getDataManager().getPlayerData(target);
        MinigameData gameData = playerData.getMinigameData(game.getCOMMAND_MAIN_NAME(), game.isTeamGame());

        player.sendMessage(Prefix.SERVER + "=== " + target.getName() + "의 " + game.getDisplayGameName() + " 통계 ===");
        if (game.isTeamGame()) {
            player.sendMessage(Prefix.SERVER + "승리: " + gameData.getWins());
            player.sendMessage(Prefix.SERVER + "패배: " + gameData.getLosses());
            player.sendMessage(Prefix.SERVER + "승률: " + String.format("%.2f%%", gameData.getWinRate() * 100));
        } else {
            for (int i = 1; i <= 10; i++) {
                player.sendMessage(Prefix.SERVER.toString() + i + "등: " + gameData.getRankCount(i) + "회");
            }
        }
        player.sendMessage(Prefix.SERVER + "총 게임 수: " + gameData.getTotalGames());
        return true;
    }

    private boolean handleSetGameSettings(Player player, Minigame game, String type, String[] args) {
        if (args.length < 4) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set " + type + " <숫자>");
            return true;
        }

        try {
            int value = Integer.parseInt(args[3]);
            switch (type) {
                case "min":
                    game.getConfig().setMinPlayers(value);
                    player.sendMessage(Prefix.SERVER + "최소 플레이어 수가 " + value + "로 설정되었습니다.");
                    break;
                case "max":
                    game.getConfig().setMaxPlayers(value);
                    player.sendMessage(Prefix.SERVER + "최대 플레이어 수가 " + value + "로 설정되었습니다.");
                    break;
                case "time":
                    game.setGameTimeLimit(value);
                    player.sendMessage(Prefix.SERVER + "게임 제한 시간이 " + value + "초로 설정되었습니다.");
                    break;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Prefix.SERVER + "올바른 숫자를 입력해주세요.");
        }
        return true;
    }

    private void sendGeneralHelpMessage(Player player) {
        player.sendMessage(Prefix.SERVER + "=== 미니게임 명령어 ===");
        player.sendMessage(USAGE_PREFIX + "<게임이름> [하위명령어]");
        player.sendMessage(Prefix.SERVER + "사용 가능한 게임:");
        for (Minigame game : plugin.getMinigameList()) {
            if (game.getState() != MinigameState.DISABLED) {
                player.sendMessage(Prefix.SERVER + "- " + game.getCOMMAND_MAIN_NAME() + ": " + game.getDisplayGameName());
            }
        }
        player.sendMessage(Prefix.SERVER + "각 게임의 자세한 명령어는 /minigame <게임이름>을 입력하세요.");
    }

    private void sendHelpMessage(Player player, Minigame game, boolean isDetailed) {
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 명령어 ===");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " join - 게임에 참가합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " quit - 게임에서 나갑니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " info - 게임 정보를 확인합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " rules - 게임 규칙을 확인합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " stats [플레이어명] - 게임 통계를 확인합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " top - 상위 플레이어 목록을 확인합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " map - 맵 목록을 확인합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " mapinfo <맵이름> - 특정 맵의 정보를 확인합니다.");
        if (game.isTeamGame()) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " tc (또는 teamchat) - 팀 채팅을 켜거나 끕니다.");
        }

        if (player.hasPermission(PERMISSION_ADMIN)) {
            sendAdminHelpMessage(player, game, isDetailed);
        }
    }

    private void sendRestoreHelpMessage(Player player, Minigame game) {
        player.sendMessage(Prefix.SERVER + "=== 복구 시스템 명령어 ===");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " restore setregion <맵이름> - 복구 영역을 설정합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " restore start <맵이름> - 맵 복구를 시작합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " restore cancel <맵이름> - 맵 복구를 취소합니다.");
    }

    private void sendAdminHelpMessage(Player player, Minigame game, boolean isDetailed) {
        player.sendMessage(Prefix.SERVER + "=== 관리자 명령어 ===");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " start - 게임을 강제로 시작합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " stop - 게임을 강제로 종료합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set lobby - 현재 위치를 게임 로비로 설정합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set spawn <맵이름> <teamType/teamName> [teamNumber] - 현재 위치를 게임 스폰 위치로 추가합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set map <맵이름> - 새 맵을 추가합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set mapicon <맵이름> - 맵 아이콘을 설정합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set min <숫자> - 최소 플레이어 수를 설정합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set max <숫자> - 최대 플레이어 수를 설정합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set time <초> - 게임 시간을 설정합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " restore - 복구 시스템 명령어를 확인합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " list - 게임 스폰 위치 목록을 확인합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " remove spawn <맵이름> <teamType/teamName> [teamNumber] - 특정 맵의 스폰 위치를 제거합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " remove lobby - 게임 로비를 제거합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " remove map <맵이름> - 특정 맵과 관련된 모든 설정을 제거합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " reload - 게임 설정을 다시 로드합니다.");
    }

    private boolean hasAdminPermission(Player player) {
        if (!player.hasPermission(PERMISSION_ADMIN)) {
            player.sendMessage(Prefix.SERVER + "이 명령어를 사용할 권한이 없습니다.");
            return false;
        }
        return true;
    }
}