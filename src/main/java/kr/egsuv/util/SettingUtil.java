package kr.egsuv.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

public class SettingUtil {

    // 플레이어가 바라보고 있는 블록의 위치를 반환합니다.
    public static Location getTargetBlockLocation(Player player, int maxDistance) {
        BlockIterator blockIterator = new BlockIterator(player, maxDistance);
        Location targetBlockLocation = null;

        while (blockIterator.hasNext()) {
            Location blockLocation = blockIterator.next().getLocation();
            Material blockType = blockLocation.getBlock().getType();

            if (blockType != Material.AIR) { // 공기가 아닌 블록에 도달하면 해당 위치를 반환
                targetBlockLocation = blockLocation;
                break;
            }
        }

        return targetBlockLocation;
    }
}