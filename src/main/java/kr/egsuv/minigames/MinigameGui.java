package kr.egsuv.minigames;

import kr.egsuv.util.ItemUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MinigameGui {

    private ItemStack miniGameGuiItem;

    public MinigameGui() {
        miniGameGuiItem = ItemUtils.createItem(Material.NETHER_STAR, 1, "§a게임 메뉴",
                "§7§l| §f우클릭 시 게임 메뉴를 확인합니다.");
    }

    public ItemStack getMiniGameGuiItem() {
        return miniGameGuiItem;
    }
}
