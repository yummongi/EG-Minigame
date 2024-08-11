package kr.egsuv.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GlobalEventListener implements Listener {

    private final EGServerMain plugin = EGServerMain.getInstance();

    // 아이템 드롭 방지 및 제한된 아이템 드롭 방지
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }
        Minigame currentGame = getCurrentGame(player);
        ItemStack item = event.getItemDrop().getItemStack();
        if (isRestrictedItem(item)) {
            event.setCancelled(true);
        }
        if (event.getItemDrop().getItemStack().getItemMeta().hasDisplayName()) {
            player.sendActionBar(Component.text("§c해당 아이템은 버릴 수 없습니다."));
            event.setCancelled(true);
        }
        if (currentGame != null && !currentGame.isItemDropAllowed() ||
                plugin.getPlayerListLocation(player).equals("로비") ||
                plugin.getPlayerListLocation(player).equals("게임로비")) {
            event.setCancelled(true);
        }


    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        event.setCancelled(true);
    }

    // 인벤토리 아이템 이동 방지 및 제한된 아이템 이동 방지
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (player.isOp()) {
            return;
        }
        Minigame currentGame = getCurrentGame(player);

        if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            event.setCancelled(true);
            return;
        }

        // GUI 인벤토리에서의 모든 클릭 이벤트 취소
        if (event.getView().title().examinableName().startsWith("§0")) { //depre
            event.setCancelled(true);
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item != null && isRestrictedItem(item) ||
                plugin.getPlayerListLocation(player).equals("로비") ||
                plugin.getPlayerListLocation(player).equals("게임로비")) {
            event.setCancelled(true);
        }

        // 플레이어 인벤토리 내에서의 이동만 허용
        if (currentGame != null && !currentGame.isItemMoveAllowed()) {
            if (event.getClickedInventory() != null && event.getClickedInventory().getType() != InventoryType.PLAYER) {
                event.setCancelled(true);
                return;
            }
        }

    }

    // 블록 파괴 방지
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Minigame currentGame = getCurrentGame(player);
        if (currentGame != null) {
            if (!currentGame.isBlockBreakAllowed() && !player.isOp()) {
                event.setCancelled(true);
            } else if (currentGame.useBlockRestore) {
                currentGame.handleBlockBreak(event.getBlock());
            }
        } else if (plugin.getPlayerListLocation(player).equals("로비") ||
                plugin.getPlayerListLocation(player).equals("게임로비")) {
            if (!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }


    // 블록 설치 방지
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Minigame currentGame = getCurrentGame(player);
        if (currentGame != null) {
            if (!currentGame.isBlockPlaceAllowed() && !player.isOp()) {
                event.setCancelled(true);
            } else if (currentGame.useBlockRestore) {
                currentGame.handleBlockPlace(event.getBlockPlaced().getState());
            }
        } else if (plugin.getPlayerListLocation(player).equals("로비") ||
                plugin.getPlayerListLocation(player).equals("게임로비")) {
            if (!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }

    // 아이템 줍기 방지
    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isOp()) {
            return;
        }
        Minigame currentGame = getCurrentGame(player);
        if (currentGame != null && !currentGame.isItemPickupAllowed() ||
                plugin.getPlayerListLocation(player).equals("로비") ||
                plugin.getPlayerListLocation(player).equals("게임로비")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.BLAZE_ROD && player.hasPermission("minigame.admin")) {
            Minigame currentGame = getCurrentGame(player);
            if (currentGame != null && currentGame.useBlockRestore) {
                // BlockRestoreManager의 onPlayerInteract 메소드를 직접 호출
                plugin.getBlockRestoreManager().onPlayerInteract(event);
            }
        }
    }

    // 제한된 아이템 여부 확인 (버릴 수 없고 옮길 수 없음)
    private boolean isRestrictedItem(ItemStack item) {

        if (item == null) return false;

        ItemMeta meta = item.getItemMeta();
        return item.getType() == Material.NETHER_STAR || item.getType() == Material.ENCHANTED_BOOK ||
                (meta != null && meta.hasDisplayName());
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

    @EventHandler
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        switch (event.getEntity().getType()) {
            case ARMOR_STAND:
            case TNT:
            case MINECART:
                event.getEntity().remove(); // 이벤트를 취소하는 대신, 엔티티를 월드에서 제거합니다.
                break;
            default:
                // 다른 엔티티 유형에 대해서는 아무것도 하지 않습니다.
                break;
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        Material blockType = event.getBlock().getType();

        if (blockType == Material.ICE || blockType == Material.SNOW || blockType == Material.SNOW_BLOCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMouseClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        // Spectator 모드에서 왼쪽 클릭 시 기능 제한
        if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && player.getGameMode() == GameMode.SPECTATOR) {
            player.sendTitle("", "§c지원하지 않는 기능입니다.", 0, 60, 0);
            event.setCancelled(true);
            return;
        }

        // Ender Chest나 Anvil 클릭 시 이벤트 취소
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null && (clickedBlock.getType() == Material.ENDER_CHEST || clickedBlock.getType() == Material.ANVIL)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // 플레이어가 관전 텔레포트를 시도할 때 이를 차단
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            event.getPlayer().sendTitle("", "§e지원하지 않는 기능입니다.", 0, 60, 0);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        // 로비에 있는 플레이어의 배고픔이 줄어들지 않게 설정
        if (event.getEntity() instanceof Player player) {
            if (plugin.getPlayerListLocation(player).equals("로비") || plugin.getPlayerListLocation(player).equals("게임로비")) {
                event.setFoodLevel(20); // 배고픔을 항상 최대치로 설정
                event.setCancelled(true); // 이벤트를 취소하여 배고픔 변화가 발생하지 않도록 함
            }
        }
    }
}