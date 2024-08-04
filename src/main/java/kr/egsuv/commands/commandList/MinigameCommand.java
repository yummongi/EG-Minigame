package kr.egsuv.commands.commandList;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.Command;
import kr.egsuv.data.MinigameData;
import kr.egsuv.data.PlayerData;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameState;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class MinigameCommand implements Command {

    private final EGServerMain plugin = EGServerMain.getInstance();

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
            sendMinigameHelpMessage(player, game);
            return true;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "join":
                game.joinGame(player);
                return true;
            case "quit":
                game.gameQuitPlayer(player);
                return true;
            case "start":
                if (player.hasPermission("minigame.admin")) {
                    game.forceStart();
                } else {
                    player.sendMessage(Prefix.SERVER + "이 명령어를 사용할 권한이 없습니다.");
                }
                return true;
            case "set":
                handleSetCommand(player, game, args);
                return true;
            case "remove":
                handleRemoveCommand(player, game, args);
                return true;
            case "list":
                listSpawnLocations(player, game);
                return true;
            case "info":
                showGameInfo(player, game);
                return true;
            case "rules":
                showGameRules(player, game);
                return true;
            case "top":
                showTopPlayers(player, game);
                return true;
            case "stats":
                showPlayerStats(player, game, args);
                return true;
            case "setmin":
            case "setmax":
            case "settime":
                handleSetGameSettings(player, game, subCommand, args);
                return true;
            default:
                sendHelpMessage(player, game);
                return true;
        }
    }



    private void sendGeneralHelpMessage(Player player) {
        player.sendMessage(Prefix.SERVER + "=== 미니게임 명령어 ===");
        player.sendMessage(Prefix.SERVER + "사용법: /minigame <게임이름> [하위명령어]");
        player.sendMessage(Prefix.SERVER + "사용 가능한 게임:");
        for (Minigame game : plugin.getMinigameList()) {
            if (game.getState() != MinigameState.DISABLED) {
                player.sendMessage(Prefix.SERVER + "- " + game.getCOMMAND_MAIN_NAME() + ": " + game.getDisplayGameName());
            }
        }
    }
    private void listAllMinigames(Player player) {
        player.sendMessage(Prefix.SERVER + "=== 사용 가능한 미니게임 목록 ===");
        for (Minigame game : plugin.getMinigameList()) {
            player.sendMessage(Prefix.SERVER + "- " + game.getDisplayGameName() + " (" + game.getCOMMAND_MAIN_NAME() + ")");
        }
    }

    private void showGameInfo(Player player, Minigame game) {
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 정보 ===");
        player.sendMessage(Prefix.SERVER + "현재 상태: " + game.getState());
        player.sendMessage(Prefix.SERVER + "참가자 수: " + game.getPlayers().size() + "/" + game.getMAX_PLAYER());
        player.sendMessage(Prefix.SERVER + "최소 시작 인원: " + game.getMIN_PLAYER());
        player.sendMessage(Prefix.SERVER + "게임 시간 제한: " + game.getGameTimeLimit() + "초");
    }

    private void showGameRules(Player player, Minigame game) {
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 규칙 ===");
        // 각 게임별로 규칙을 정의하고 표시하는 메서드를 Minigame 클래스에 추가해야 합니다.
        game.showRules(player);
    }

    private void showTopPlayers(Player player, Minigame game) {
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 상위 플레이어 ===");
        List<Map.Entry<String, Double>> topPlayers = plugin.getRankingManager().getTopPlayers(game.getCOMMAND_MAIN_NAME(), 10);
        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<String, Double> entry = topPlayers.get(i);
            player.sendMessage(String.format("%d. %s - %.2f점", i + 1, entry.getKey(), entry.getValue()));
        }
    }

    private void showPlayerStats(Player player, Minigame game, String[] args) {
        Player target = player;
        if (args.length > 2) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                player.sendMessage(Prefix.SERVER + "해당 플레이어를 찾을 수 없습니다.");
                return;
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
    }

    private void handleSetGameSettings(Player player, Minigame game, String subCommand, String[] args) {
        if (!player.hasPermission("minigame.admin")) {
            player.sendMessage(Prefix.SERVER + "이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(Prefix.SERVER + "사용법: /minigame " + game.getCOMMAND_MAIN_NAME() + " " + subCommand + " <숫자>");
            return;
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
    }


    private void handleSetCommand(Player player, Minigame game, String[] args) {
        if (!player.hasPermission("minigame.admin")) {
            player.sendMessage(Prefix.SERVER + "이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(Prefix.SERVER + "사용법: /minigame " + game.getCOMMAND_MAIN_NAME() + " set <lobby|spawn> [solo|red|blue]");
            return;
        }

        String type = args[2].toLowerCase();
        Location playerLocation = player.getLocation();

        switch (type) {
            case "lobby":
                game.getConfig().setGameLobbyLocation(playerLocation);
                player.sendMessage(Prefix.SERVER + "게임 로비 위치가 설정되었습니다.");
                break;
            case "spawn":
                if (args.length < 4) {
                    player.sendMessage(Prefix.SERVER + "사용법: /minigame " + game.getCOMMAND_MAIN_NAME() + " set spawn <solo|red|blue>");
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
                    default:
                        player.sendMessage(Prefix.SERVER + "알 수 없는 스폰 유형입니다. solo, red, 또는 blue를 사용하세요.");
                        break;
                }
                break;
            default:
                player.sendMessage(Prefix.SERVER + "알 수 없는 설정 유형입니다. lobby 또는 spawn을 사용하세요.");
                break;
        }
    }

    private void handleRemoveCommand(Player player, Minigame game, String[] args) {
        if (args.length < 4 || !args[2].equalsIgnoreCase("spawn")) {
            player.sendMessage(Prefix.SERVER + "사용법: /minigame " + game.getCOMMAND_MAIN_NAME() + " remove spawn <인덱스>");
            return;
        }
        removeSpawnLocation(player, game, args[3]);
    }

    private void listSpawnLocations(Player player, Minigame game) {
        List<Location> locations = game.getConfig().getSoloLocations();
        player.sendMessage(Prefix.SERVER + "게임 스폰 위치 목록:");
        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            player.sendMessage(String.format("%d: %s, %.2f, %.2f, %.2f", i, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
        }
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

    private void sendMinigameHelpMessage(Player player, Minigame game) {
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 명령어 ===");
        player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " join - 게임에 참가합니다.");
        player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " quit - 게임에서 나갑니다.");
        player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " info - 게임 정보를 확인합니다.");
        player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " rules - 게임 규칙을 확인합니다.");
        player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " stats - 자신의 게임 통계를 확인합니다.");
        if (player.hasPermission("minigame.admin")) {
            player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " start - 게임을 강제로 시작합니다.");
            player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " set <lobby|spawn> - 게임 위치를 설정합니다.");
            player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " setmin <숫자> - 최소 플레이어 수를 설정합니다.");
            player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " setmax <숫자> - 최대 플레이어 수를 설정합니다.");
            player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " settime <초> - 게임 시간을 설정합니다.");
        }
    }

    private void sendHelpMessage(Player player, Minigame game) {
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 명령어 ===");
        player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " join - 게임에 참가합니다.");
        player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " quit - 게임에서 나갑니다.");
        if (player.hasPermission("minigame.admin")) {
            player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " start - 게임을 강제로 시작합니다.");
            player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " set lobby - 현재 위치를 게임 로비로 설정합니다.");
            player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " set spawn - 현재 위치를 게임 스폰 위치로 추가합니다.");
            player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " list - 게임 스폰 위치 목록을 확인합니다.");
            player.sendMessage(Prefix.SERVER + "/minigame " + game.getCOMMAND_MAIN_NAME() + " remove spawn <인덱스> - 특정 게임 스폰 위치를 제거합니다.");
        }
    }

}