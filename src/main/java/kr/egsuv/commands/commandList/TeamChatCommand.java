package kr.egsuv.commands.commandList;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.Command;
import kr.egsuv.minigames.Minigame;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class TeamChatCommand implements Command {

    private final EGServerMain plugin = EGServerMain.getInstance();

    @Override
    public boolean executeCommand(Player player, String[] message) {

        Minigame game = plugin.getCurrentGame(player);
        toggleTeamChat(player, game);
        return false;
    }

    private boolean toggleTeamChat(Player player, Minigame game) {
        if (game == null || !game.isTeamGame() || !game.getPlayers().contains(player)) {
            player.sendMessage(Prefix.SERVER + "팀 채팅은 팀 게임 중에만 사용할 수 있습니다.");
            return true;
        }
        game.toggleTeamChat(player);
        return true;
    }
}
