package kr.egsuv.minigames;

import kr.egsuv.util.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public abstract class MiniGames {

    private final int MIN_PLAYER;
    private final int MAX_PLAYER;
    private final String displayGameName;
    private final ItemStack helperItem;

    private Set<Player> waitingPlayers = new HashSet<>();


    public MiniGames(int MIN_PLAYER, int MAX_PLAYER, String displayGameName) {
        this.MIN_PLAYER = MIN_PLAYER;
        this.MAX_PLAYER = MAX_PLAYER;
        this.displayGameName = displayGameName;

        this.helperItem = ItemUtils.createItem(Material.ENCHANTED_BOOK, 1, "§7[ §f게임 §a튜토리얼 §f읽기 §7]",
                "§7§l| §f우클릭 시 게임 하는 방법을 알아볼 수 있습니다.");

    }

    public void joinGame() {

    }

    // 게임 시작
    public abstract void startGame();

    //게임 종료
    public abstract void endGame();

    //게임 나감
    public abstract void gameQuitPlayer();


    public int getMIN_PLAYER() {
        return MIN_PLAYER;
    }

    public int getMAX_PLAYER() {
        return MAX_PLAYER;
    }

    public String getDisplayGameName() {
        return displayGameName;
    }
}
