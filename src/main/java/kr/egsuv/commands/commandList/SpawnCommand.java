package kr.egsuv.commands.commandList;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.Command;
import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameGui;
import kr.egsuv.util.PotionUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class SpawnCommand implements Command {

    private final EGServerMain plugin = EGServerMain.getInstance();
    private MinigameGui miniGamesGui;

    public SpawnCommand(MinigameGui miniGamesGui) {
        this.miniGamesGui = miniGamesGui;
    }

    @Override
    public boolean executeCommand(Player player, String[] message) {
        if (player.isOp() && message.length > 0 && message[0].equals("set")) {
            setSpawnLocation(player);
            return true;
        }

        teleportToSpawn(player);
        return true;
    }

    private void initPlayerState(Player player) {
        // ActionBar, Title 초기화
        player.sendActionBar(Component.empty());

        // 활성화된 모든 포션 효과를 가져옵니다.
        for (PotionEffect effect : player.getActivePotionEffects()) {
            // 각 포션 효과를 제거합니다.
            player.removePotionEffect(effect.getType());
        }
        if (player.isOp()) {
            player.setGameMode(GameMode.CREATIVE);
            player.setAllowFlight(true);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setLevel(0);
            player.setExp(0);
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
        }

        player.setPlayerTime(3000, false);
        player.setGravity(true);
        player.setPlayerWeather(WeatherType.CLEAR);
        PotionUtils.applyPotionEffect(player, PotionEffectType.REGENERATION, 72000, 1);

        player.setSneaking(false);
        player.setPlayerWeather(WeatherType.CLEAR);

        // 무적 모드 해제
        player.setInvulnerable(false);
    }

    private void initPlayerInventory(Player player) {
        player.closeInventory();
        PlayerInventory playerInventory = player.getInventory();
        playerInventory.clear();
        playerInventory.setArmorContents(null);

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
        player.sendMessage(Prefix.SERVER + "§f현재 위치로 §c스폰 설정§f이 완료 되었습니다.");
    }

    public boolean teleportToSpawn(Player player) {

        for (Minigame minigame : plugin.getMinigameList()) {
            if (minigame.getPlayers().contains(player)) {
                minigame.gameQuitPlayer(player);
                if (minigame.getTimerBossBar() != null) {
                    minigame.getTimerBossBar().removePlayer(player);
                    minigame.getTimerBossBar().removePlayer(player);
                }
                break;
            }
        }
        initPlayer(player);
        plugin.setPlayerList(player, "로비");
        return player.teleport(plugin.getSpawnConfigManager().getSpawnLocation());
    }
}

