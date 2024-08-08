package kr.egsuv.ranking;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.minigames.Minigame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillStreakManager {
    private final Minigame minigame;
    private final Map<UUID, Integer> killStreaks = new HashMap<>();
    private final Map<UUID, Long> lastKillTime = new HashMap<>();
    private static final long KILL_STREAK_TIMEOUT = 15000; // 15 seconds

    private final EGServerMain plugin = EGServerMain.getInstance();

    public KillStreakManager(Minigame minigame) {
        this.minigame = minigame;
        startKillStreakTimeoutChecker();
    }

    public void handleKill(Player killer, Player victim) {
        resetKillStreak(victim);

        UUID killerUUID = killer.getUniqueId();
        int streak = killStreaks.getOrDefault(killerUUID, 0) + 1;
        killStreaks.put(killerUUID, streak);
        lastKillTime.put(killerUUID, System.currentTimeMillis());

        if (streak == 3 || streak == 4 || streak == 5) {
            broadcastKillStreak(killer, streak);
        }

        minigame.applyCustomKillStreakBonus(killer, streak);
    }


    public void resetKillStreak(Player player) {
        killStreaks.remove(player.getUniqueId());
        lastKillTime.remove(player.getUniqueId());
        minigame.removeCustomKillStreakEffects(player);
    }

    private void broadcastKillStreak(Player killer, int streak) {
        String message = switch (streak) {
            case 3 -> "§6" + killer.getName() + "§e이(가) §c3연속 킬§e을 달성했습니다!";
            case 4 -> "§6" + killer.getName() + "§e이(가) §c4연속 킬§e로 §b날아오릅니다§e!";
            case 5 -> "§6" + killer.getName() + "§e이(가) §c5연속 킬§e로 §d영웅§e이 됩니다!";
            default -> "";
        };
        minigame.broadcastToPlayers(Component.text(message));

        // 타이틀 메시지 표시
        Title title = Title.title(
                Component.text("§c" + streak + " KILL STREAK!"),
                Component.text("§6" + killer.getName()),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        for (Player player : minigame.getPlayers()) {
            player.showTitle(title);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }


    private void startKillStreakTimeoutChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Map.Entry<UUID, Long> entry : lastKillTime.entrySet()) {
                    if (currentTime - entry.getValue() > KILL_STREAK_TIMEOUT) {
                        Player player = plugin.getServer().getPlayer(entry.getKey());
                        if (player != null) {
                            resetKillStreak(player);
                            player.sendMessage(Component.text(Prefix.SERVER + "§c15초§7가 지나 §c연속 킬 §7측정이 초기화 됩니다."));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public int getKillStreak(Player player) {
        return killStreaks.getOrDefault(player.getUniqueId(), 0);
    }
}