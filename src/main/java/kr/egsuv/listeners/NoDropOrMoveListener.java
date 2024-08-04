package kr.egsuv.listeners;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class NoDropOrMoveListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();

    // 아이템 드롭 방지 및 제한된 아이템 드롭 방지
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Minigame currentGame = getCurrentGame(player);
        if (currentGame != null && !currentGame.isItemDropAllowed()) {
            event.setCancelled(true);
        }

        ItemStack item = event.getItemDrop().getItemStack();
        if (isRestrictedItem(item)) {
            event.setCancelled(true);
        }
    }

    // 인벤토리 아이템 이동 방지 및 제한된 아이템 이동 방지
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Minigame currentGame = getCurrentGame(player);

        // GUI 인벤토리에서의 모든 클릭 이벤트 취소
        if (event.getView().getTitle().startsWith("§0")) {
            event.setCancelled(true);
            return;
        }

        // 플레이어 인벤토리 내에서의 이동만 허용
        if (currentGame != null && !currentGame.isItemMoveAllowed()) {
            if (event.getClickedInventory() != null && event.getClickedInventory().getType() != InventoryType.PLAYER) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack item = event.getCurrentItem();
        if (item != null && isRestrictedItem(item)) {
            event.setCancelled(true);
        }
    }

    // 블록 파괴 방지
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Minigame currentGame = getCurrentGame(player);
        if (currentGame != null && !currentGame.isBlockBreakAllowed()) {
            event.setCancelled(true);
        }
    }

    // 블록 설치 방지
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Minigame currentGame = getCurrentGame(player);
        if (currentGame != null && !currentGame.isBlockPlaceAllowed()) {
            event.setCancelled(true);
        }
    }

    // 아이템 줍기 방지
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        Minigame currentGame = getCurrentGame(player);
        if (currentGame != null && !currentGame.isItemPickupAllowed()) {
            event.setCancelled(true);
        }
    }

    // 제한된 아이템 여부 확인
    private boolean isRestrictedItem(ItemStack item) {
        return item.getType() == Material.NETHER_STAR || item.getType() == Material.ENCHANTED_BOOK;
    }

    // 플레이어의 현재 게임 가져오기
    private Minigame getCurrentGame(Player player) {
        String location = plugin.getPlayerListLocation(player);
        if (location != null) {
            for (Minigame minigame : plugin.getMinigameList()) {
                if (location.equalsIgnoreCase(minigame.getCOMMAND_MAIN_NAME())) {
                    return minigame;
                }
            }
        }
        return null;
    }
}