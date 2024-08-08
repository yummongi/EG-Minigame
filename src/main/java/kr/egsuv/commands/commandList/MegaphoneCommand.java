package kr.egsuv.commands.commandList;

import kr.egsuv.EGServerMain;
import kr.egsuv.chat.Prefix;
import kr.egsuv.commands.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class MegaphoneCommand implements Command {

    private final EGServerMain plugin = EGServerMain.getInstance();

    private static final int MAX_MESSAGE_LENGTH = 20;
    private static final String USAGE_MESSAGE = Prefix.SERVER + "사용법: /확성기 <메시지 [20글자 이내]> 또는 /확성기 <켜기|끄기>";
    private static final String LENGTH_ERROR_MESSAGE = Prefix.SERVER + "확성기는 20글자 이내로만 사용이 가능합니다.";
    //    private static final String PERMISSION_ERROR_MESSAGE = Rank.SERVER + "확성기를 사용할 권한이 없습니다.";

    private static final long MESSAGE_DURATION = 200L; // 7초 (20 ticks = 1초)
    private static final long UPDATE_INTERVAL = 40L; // 2초마다 업데이트

    private static final long COOLDOWN_DURATION = 30L; // 쿨타임 30초

    private final Map<UUID, Queue<String>> megaphoneMessageQueues = new HashMap<>();
    private final Map<UUID, BukkitRunnable> megaphoneTaskMap = new HashMap<>();
    private final Map<UUID, Boolean> megaphoneEnabled = new HashMap<>();
    private final Map<UUID, Long> lastUsageTime = new HashMap<>();

    @Override
    public boolean executeCommand(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(USAGE_MESSAGE);
            return true;
        }

        if (args[0].equalsIgnoreCase("끄기")) {
            setMegaphoneEnabled(player, false);
            player.sendMessage(Prefix.SERVER + "확성기 메시지를 받지 않습니다.");
            return true;
        }

        if (args[0].equalsIgnoreCase("켜기")) {
            setMegaphoneEnabled(player, true);
            player.sendMessage(Prefix.SERVER + "확성기 메시지를 받습니다.");
            return true;
        }

        if (isOnCooldown(player)) {
            long remainingTime = getRemainingCooldown(player);
            player.sendMessage(Prefix.SERVER + String.format("확성기를 다시 사용하려면 %d초 남았습니다.", remainingTime));
            return true;
        }

        String message = String.join(" ", args).trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            player.sendMessage(LENGTH_ERROR_MESSAGE);
            return true;
        }

        broadcastMegaphoneMessage(player, message);
        setLastUsageTime(player);
        return true;
    }

    private void broadcastMegaphoneMessage(Player sender, String message) {
        String formattedMessage = String.format("§c[확성기] §f%s: %s", sender.getName(), message);
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (isMegaphoneEnabled(onlinePlayer)) {
                queueMegaphoneMessage(onlinePlayer, formattedMessage);
            }
        }
    }

    private void queueMegaphoneMessage(Player player, String message) {
        UUID playerUUID = player.getUniqueId();
        megaphoneMessageQueues.computeIfAbsent(playerUUID, k -> new ConcurrentLinkedQueue<>()).offer(message);

        if (!megaphoneTaskMap.containsKey(playerUUID)) {
            startMegaphoneTask(player);
        }
    }

    private void startMegaphoneTask(Player player) {
        UUID playerUUID = player.getUniqueId();
        BukkitRunnable task = new BukkitRunnable() {
            private int count = 0;
            private String currentMessage = null;

            @Override
            public void run() {
                Queue<String> queue = megaphoneMessageQueues.get(playerUUID);
                if (queue.isEmpty() && count >= (MESSAGE_DURATION / UPDATE_INTERVAL)) {
                    megaphoneTaskMap.remove(playerUUID);
                    this.cancel();
                    return;
                }

                if (count == 0 || count >= (MESSAGE_DURATION / UPDATE_INTERVAL)) {
                    currentMessage = queue.poll();
                    count = 0;
                }

                if (currentMessage != null) {
                    Component messageComponent = LegacyComponentSerializer.legacySection().deserialize(currentMessage);
                    player.sendActionBar(messageComponent);
                    count++;
                }
            }
        };
        task.runTaskTimer(plugin, 0L, UPDATE_INTERVAL);
        megaphoneTaskMap.put(playerUUID, task);
    }

    public boolean isMegaphoneEnabled(Player player) {
        return megaphoneEnabled.getOrDefault(player.getUniqueId(), true);
    }

    public void setMegaphoneEnabled(Player player, boolean enabled) {
        megaphoneEnabled.put(player.getUniqueId(), enabled);
    }

    private boolean isOnCooldown(Player player) {
        long lastUsage = lastUsageTime.getOrDefault(player.getUniqueId(), 0L);
        return (System.currentTimeMillis() - lastUsage) < TimeUnit.SECONDS.toMillis(COOLDOWN_DURATION);
    }

    private long getRemainingCooldown(Player player) {
        long lastUsage = lastUsageTime.getOrDefault(player.getUniqueId(), 0L);
        long elapsedTime = System.currentTimeMillis() - lastUsage;
        long remainingTime = TimeUnit.SECONDS.toMillis(COOLDOWN_DURATION) - elapsedTime;
        return TimeUnit.MILLISECONDS.toSeconds(remainingTime);
    }

    private void setLastUsageTime(Player player) {
        lastUsageTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

}