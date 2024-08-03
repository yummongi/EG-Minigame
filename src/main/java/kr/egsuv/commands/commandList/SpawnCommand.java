package kr.egsuv.commands.commandList;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Rank;
import kr.egsuv.commands.Command;
import kr.egsuv.minigames.MiniGamesGui;
import kr.egsuv.util.PotionUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;


public class SpawnCommand implements Command {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private MiniGamesGui miniGamesGui;

    public SpawnCommand(MiniGamesGui miniGamesGui) {
        this.miniGamesGui = miniGamesGui;
    }

    @Override
    public boolean executeCommand(Player player, String[] message) {
        if (player.isOp() && message.length > 0 && message[0].equals("set")) {
            setSpawnLocation(player);
            return true;
        }

        plugin.setPlayerList(player, "로비");
        initPlayerState(player);
        initPlayerInventory(player);
        teleportToSpawn(player);
        return true;
    }

    private void initPlayerState(Player player) {
        if (player.isOp()) {
            player.setGameMode(GameMode.CREATIVE);
            player.setAllowFlight(true);
            player.setSneaking(true);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setSneaking(false);
            player.setLevel(0);
            player.setExp(0);
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
        }

        player.closeInventory();
        player.setPlayerTime(3000, false);
        player.setGravity(true);
        player.setPlayerWeather(WeatherType.CLEAR);
        PotionUtils.applyPotionEffect(player, PotionEffectType.REGENERATION, 72000, 1);
    }

    private void initPlayerInventory(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        playerInventory.clear();
        playerInventory.setHeldItemSlot(8);
        playerInventory.setItem(8, miniGamesGui.getMiniGameGuiItem());
    }

    public boolean initPlayer(Player player) {
        initPlayerState(player);
        initPlayerInventory(player);
        return true;
    }

    private void setSpawnLocation(Player player) {
        Location location = player.getLocation();
        plugin.getSpawnConfigManager().setSpawnLocation(location);
        player.sendMessage(Rank.SERVER + "§f현재 위치로 §c스폰 설정§f이 완료 되었습니다.");
    }

    public boolean teleportToSpawn(Player player) {
        return player.teleport(plugin.getSpawnConfigManager().getSpawnLocation());
    }
}

