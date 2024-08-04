package kr.egsuv.commands.commandList;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.Command;
import kr.egsuv.minigames.games.FirstHitGame;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class FirstHitCommand implements Command {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private final FirstHitGame game;

    public FirstHitCommand(FirstHitGame game) {
        this.game = game;
    }

    @Override
    public boolean executeCommand(Player player, String[] args) {
        if (game == null) {
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

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
                if (player.hasPermission("minigame.admin")) {
                    if (args.length < 2) {
                        player.sendMessage(Prefix.SERVER + "사용법: /firsthit set <lobby|spawn>");
                        return true;
                    }
                    handleSetCommand(player, args[1]);
                } else {
                    player.sendMessage(Prefix.SERVER + "이 명령어를 사용할 권한이 없습니다.");
                }
                return true;
            case "remove":
                if (player.hasPermission("minigame.admin")) {
                    if (args.length < 3 || !args[1].equalsIgnoreCase("spawn")) {
                        player.sendMessage(Prefix.SERVER + "사용법: /firsthit remove spawn <인덱스>");
                        return true;
                    }
                    removeSpawnLocation(player, args[2]);
                } else {
                    player.sendMessage(Prefix.SERVER + "이 명령어를 사용할 권한이 없습니다.");
                }
                return true;
            case "list":
                if (player.hasPermission("minigame.admin")) {
                    listSpawnLocations(player);
                } else {
                    player.sendMessage(Prefix.SERVER + "이 명령어를 사용할 권한이 없습니다.");
                }
                return true;
            default:
                sendHelpMessage(player);
                return true;
        }
    }

    private void handleSetCommand(Player player, String type) {
        switch (type.toLowerCase()) {
            case "lobby":
                game.getConfig().setGameLobbyLocation(player.getLocation());
                player.sendMessage(Prefix.SERVER + "게임 로비 위치가 설정되었습니다.");
                break;
            case "spawn":
                game.getConfig().addSoloLocation(player.getLocation());
                player.sendMessage(Prefix.SERVER + "게임 스폰 위치가 추가되었습니다.");
                break;
            default:
                player.sendMessage(Prefix.SERVER + "알 수 없는 설정 유형입니다. lobby 또는 spawn을 사용하세요.");
                break;
        }
    }

    private void listSpawnLocations(Player player) {
        List<Location> locations = game.getConfig().getSoloLocations();
        player.sendMessage(Prefix.SERVER + "게임 스폰 위치 목록:");
        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            player.sendMessage(String.format("%d: %s, %.2f, %.2f, %.2f", i, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
        }
    }

    private void removeSpawnLocation(Player player, String indexStr) {
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

    private void sendHelpMessage(Player player) {
        player.sendMessage(Prefix.SERVER + "=== " + game.getDisplayGameName() + " 명령어 ===");
        player.sendMessage(Prefix.SERVER + "/firsthit join - 게임에 참가합니다.");
        player.sendMessage(Prefix.SERVER + "/firsthit quit - 게임에서 나갑니다.");
        if (player.hasPermission("minigame.admin")) {
            player.sendMessage(Prefix.SERVER + "/firsthit start - 게임을 강제로 시작합니다.");
            player.sendMessage(Prefix.SERVER + "/firsthit set lobby - 현재 위치를 게임 로비로 설정합니다.");
            player.sendMessage(Prefix.SERVER + "/firsthit set spawn - 현재 위치를 게임 스폰 위치로 추가합니다.");
            player.sendMessage(Prefix.SERVER + "/firsthit list - 게임 스폰 위치 목록을 확인합니다.");
            player.sendMessage(Prefix.SERVER + "/firsthit remove spawn <인덱스> - 특정 게임 스폰 위치를 제거합니다.");
        }
    }
}