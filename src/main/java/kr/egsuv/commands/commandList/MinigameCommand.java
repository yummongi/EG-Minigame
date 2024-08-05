package kr.egsuv.commands.commandList;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.Command;
import kr.egsuv.data.MinigameData;
import kr.egsuv.data.PlayerData;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameState;
import kr.egsuv.minigames.TeamType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

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
            case "remove":
                return handleRemoveCommand(player, game, args);
            case "list":
                return listSpawnLocations(player, game);
            case "info":
                return showGameInfo(player, game);
            case "rules":
                return showGameRules(player, game);
            case "top":
                return showTopPlayers(player, game);
            case "stats":
                return showPlayerStats(player, game, args);
            case "setmin":
            case "setmax":
            case "settime":
                return handleSetGameSettings(player, game, subCommand, args);
            case "teamchat":
            case "tc":
                return toggleTeamChat(player, game);
            default:
                sendHelpMessage(player, game, true);
                return true;
        }
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
        game.gameQuitPlayer(player);
        return true;
    }

    private boolean startGame(Player player, Minigame game) {
        if (!hasAdminPermission(player)) return true;
        game.forceStart();
        return true;
    }

    private boolean stopGame(Player player, Minigame game) {
        if (!hasAdminPermission(player)) return true;
        game.endGame();
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
        if (args.length < 5) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set <lobby|spawn> <teamType> <teamNumber>");
            return true;
        }

        String type = args[2].toLowerCase();
        String teamType = args[3].toLowerCase();
        int teamNumber;
        try {
            teamNumber = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage(Prefix.SERVER + "팀 번호는 숫자여야 합니다.");
            return true;
        }

        Location playerLocation = player.getLocation();

        switch (type) {
            case "lobby":
                game.getConfig().setGameLobbyLocation(playerLocation);
                player.sendMessage(Prefix.SERVER + "게임 로비 위치가 설정되었습니다.");
                break;
            case "spawn":
                if (teamType.equals("solo")) {
                    game.getConfig().addSoloLocation(playerLocation);
                    player.sendMessage(Prefix.SERVER + "개인전 스폰 위치가 추가되었습니다.");
                } else {
                    game.getConfig().addTeamSpawnLocation(teamType, teamNumber, playerLocation);
                    player.sendMessage(Prefix.SERVER + teamType + " " + teamNumber + "팀의 스폰 위치가 추가되었습니다.");
                }
                break;
            default:
                player.sendMessage(Prefix.SERVER + "알 수 없는 설정 유형입니다. lobby 또는 spawn을 사용하세요.");
                break;
        }
        return true;
    }

    private void handleSetSpawn(Player player, Minigame game, String[] args, Location playerLocation) {
        if (args.length < 4) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set spawn <solo|red|blue|teamtype|teamspawn>");
            return;
        }
        String spawnType = args[3].toLowerCase();
        switch (spawnType) {
            case "solo":
                game.getConfig().addSoloLocation(playerLocation);
                player.sendMessage(Prefix.SERVER + "개인전 스폰 위치가 추가되었습니다.");
                break;
            case "red":
                game.getConfig().addRedTeamLocation(playerLocation);
                player.sendMessage(Prefix.SERVER + "레드팀 스폰 위치가 추가되었습니다.");
                break;
            case "blue":
                game.getConfig().addBlueTeamLocation(playerLocation);
                player.sendMessage(Prefix.SERVER + "블루팀 스폰 위치가 추가되었습니다.");
                break;
            case "teamtype":
                setTeamType(player, game, args);
                break;
            case "teamspawn":
                setTeamSpawn(player, game, args);
                break;
            default:
                player.sendMessage(Prefix.SERVER + "알 수 없는 스폰 유형입니다. solo, red, blue, teamtype, 또는 teamspawn을 사용하세요.");
                break;
        }
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

    private void setTeamSpawn(Player player, Minigame game, String[] args) {
        if (args.length < 5) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set spawn teamspawn <팀이름>");
            return;
        }
        game.getConfig().setTeamLocation(args[4], player.getLocation());
        player.sendMessage(Prefix.SERVER + args[4] + " 팀의 스폰 위치가 설정되었습니다.");
    }

    private boolean handleRemoveCommand(Player player, Minigame game, String[] args) {
        if (!hasAdminPermission(player)) return true;
        if (args.length < 4 || !args[2].equalsIgnoreCase("spawn")) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " remove spawn <인덱스>");
            return true;
        }
        removeSpawnLocation(player, game, args[3]);
        return true;
    }

    private boolean listSpawnLocations(Player player, Minigame game) {
        List<Location> locations = game.getConfig().getSoloLocations();
        player.sendMessage(Prefix.SERVER + "게임 스폰 위치 목록:");
        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            player.sendMessage(String.format("%d: %s, %.2f, %.2f, %.2f", i, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
        }
        return true;
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

    private boolean handleSetGameSettings(Player player, Minigame game, String subCommand, String[] args) {
        if (!hasAdminPermission(player)) return true;
        if (args.length < 3) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " " + subCommand + " <숫자>");
            return true;
        }

        try {
            int value = Integer.parseInt(args[2]);
            switch (subCommand) {
                case "setmin":
                    game.getConfig().setMinPlayers(value);
                    player.sendMessage(Prefix.SERVER + "최소 플레이어 수가 " + value + "로 설정되었습니다.");
                    break;
                case "setmax":
                    game.getConfig().setMaxPlayers(value);
                    player.sendMessage(Prefix.SERVER + "최대 플레이어 수가 " + value + "로 설정되었습니다.");
                    break;
                case "settime":
                    game.setGameTimeLimit(value);
                    player.sendMessage(Prefix.SERVER + "게임 제한 시간이 " + value + "초로 설정되었습니다.");
                    break;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Prefix.SERVER + "올바른 숫자를 입력해주세요.");
        }
        return true;
    }

    private void removeSpawnLocation(Player player, Minigame game, String indexStr) {
        try {
            int index = Integer.parseInt(indexStr);
            game.getConfig().removeSoloLocation(index);
            player.sendMessage(Prefix.SERVER + "인덱스 " + index + "의 스폰 위치가 제거되었습니다.");
        } catch (NumberFormatException e) {
            player.sendMessage(Prefix.SERVER + "올바른 숫자를 입력해주세요.");
        } catch (IndexOutOfBoundsException e) {
            player.sendMessage(Prefix.SERVER + "올바른 인덱스 범위를 입력해주세요.");
        }
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
        if (game.isTeamGame()) {
            player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " tc (또는 teamchat) - 팀 채팅을 켜거나 끕니다.");
        }

        if (player.hasPermission(PERMISSION_ADMIN)) {
            sendAdminHelpMessage(player, game, isDetailed);
        }
    }

    private void sendAdminHelpMessage(Player player, Minigame game, boolean isDetailed) {
        player.sendMessage(Prefix.SERVER + "=== 관리자 명령어 ===");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " start - 게임을 강제로 시작합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " stop - 게임을 강제로 종료합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set lobby - 현재 위치를 게임 로비로 설정합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set spawn <teamType> <teamNumber> - 현재 위치를 게임 스폰 위치로 추가합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " set teamtype <solo|duo|triple|squad> - 팀 타입을 설정합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " list - 게임 스폰 위치 목록을 확인합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " remove spawn <인덱스> - 특정 게임 스폰 위치를 제거합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " setmin <숫자> - 최소 플레이어 수를 설정합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " setmax <숫자> - 최대 플레이어 수를 설정합니다.");
        player.sendMessage(USAGE_PREFIX + game.getCOMMAND_MAIN_NAME() + " settime <초> - 게임 시간을 설정합니다.");
    }

    private boolean hasAdminPermission(Player player) {
        if (!player.hasPermission(PERMISSION_ADMIN)) {
            player.sendMessage(Prefix.SERVER + "이 명령어를 사용할 권한이 없습니다.");
            return false;
        }
        return true;
    }
}