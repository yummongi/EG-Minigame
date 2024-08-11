package kr.egsuv.minigames.ability;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public abstract class Ability implements Listener {
    protected final Minigame game;
    protected final Player player;
    protected final String abilityName;
    protected int cooldownPrimary = 0;
    protected int cooldownSecondary = 0;
    protected int cooldownPassive = 0;
    protected int cooldownItemKit = 300;

    private boolean isPrimarySkillOnCooldown = false;
    private boolean isSecondarySkillOnCooldown = false;

    private Map<SkillType, CooldownInfo> cooldowns = new HashMap<>();
    private Map<SkillType, Long> cooldownEndTimes = new HashMap<>();
    private BukkitRunnable cooldownTask;

    private EGServerMain plugin = EGServerMain.getInstance();

    public Ability(Minigame game, Player player, String abilityName) {
        this.game = game;
        this.player = player;
        this.abilityName = abilityName;
        startCooldownTask();
    }

    public abstract void primarySkill();
    public abstract void secondarySkill();
    public abstract void passiveSkill();
    public abstract void itemSupply();

    public void showHelp() {
        player.sendMessage(Component.text("========== " + abilityName + " ==========").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("주스킬 (좌클릭): ").color(NamedTextColor.YELLOW)
                .append(Component.text(getPrimaryDescription()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("보조스킬 (우클릭): ").color(NamedTextColor.YELLOW)
                .append(Component.text(getSecondaryDescription()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("패시브: ").color(NamedTextColor.YELLOW)
                .append(Component.text(getPassiveDescription()).color(NamedTextColor.WHITE)));
    }


    public abstract String getPrimaryDescription();
    public abstract String getSecondaryDescription();
    public abstract String getPassiveDescription();

    protected void startCooldown(SkillType skillType) {
        int cooldown = getCooldownForSkillType(skillType);
        long endTime = System.currentTimeMillis() + (cooldown * 1000L);
        cooldowns.put(skillType, new CooldownInfo(cooldown, endTime));

        if (skillType == SkillType.PRIMARY) {
            isPrimarySkillOnCooldown = true;
        } else if (skillType == SkillType.SECONDARY) {
            isSecondarySkillOnCooldown = true;
        }
    }

    private int getCooldownForSkillType(SkillType skillType) {
        switch (skillType) {
            case PRIMARY:
                return cooldownPrimary;
            case SECONDARY:
                return cooldownSecondary;
            case ITEM_KIT:
                return cooldownItemKit;
            default:
                return 0;
        }
    }

    private void startCooldownTask() {
        cooldownTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateCooldownDisplay();
            }
        };
        cooldownTask.runTaskTimer(EGServerMain.getInstance(), 0L, 10L); // 0.5초마다 업데이트
    }

    private void updateCooldownDisplay() {
        long currentTime = System.currentTimeMillis();
        StringBuilder message = new StringBuilder();

        cooldowns.entrySet().removeIf(entry -> {
            SkillType skillType = entry.getKey();
            CooldownInfo cooldownInfo = entry.getValue();

            if (currentTime >= cooldownInfo.endTime) {
                message.append(skillType).append(": §a✔ 준비완료  ");

                // 쿨타임 종료 후 상태 업데이트
                if (skillType == SkillType.PRIMARY) {
                    isPrimarySkillOnCooldown = false;
                } else if (skillType == SkillType.SECONDARY) {
                    isSecondarySkillOnCooldown = false;
                }

                return true; // 쿨다운이 끝난 경우 맵에서 제거
            } else {
                long remainingTime = (cooldownInfo.endTime - currentTime) / 1000; // 초 단위
                String timeDisplay = formatTime(remainingTime);
                String progressBar = createProgressBar(cooldownInfo.totalCooldown - remainingTime, cooldownInfo.totalCooldown);
                message.append(skillType).append(": ").append(progressBar).append(" ").append(timeDisplay).append("  ");
                return false; // 아직 쿨다운이 진행 중인 경우
            }
        });

        if (message.length() > 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message.toString().trim()));
        }
    }



    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    private String createProgressBar(long elapsed, long total) {
        int barLength = 10;
        int filledLength = (int) ((elapsed * barLength) / total);
        String filledBar = StringUtils.repeat("█", filledLength);
        String emptyBar = StringUtils.repeat("░", barLength - filledLength);
        return "§a" + filledBar + "§7" + emptyBar;
    }

    public void initCoolDowns() {
        if (cooldownTask != null) {
            cooldownTask.cancel();
        }
        cooldowns.clear(); // 쿨다운 데이터 초기화
    }



    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if(checkPlayer(player) == false) return;

        if (player.equals(this.player) && event.isSneaking()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getType() == Material.BLAZE_ROD) {
                showHelp();
            }
        }
    }

    protected boolean checkPlayer(Player player) {
        return player.getUniqueId().equals(this.player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if(checkPlayer(player) == false) return;
        Player player = event.getPlayer();

        if (player.equals(this.player) && event.getItemDrop().getItemStack().getType() == Material.BLAZE_ROD) {
            showHelp();
            event.setCancelled(true); // 아이템이 드롭되지 않도록 이벤트 취소
        }
    }

    public boolean canUsePrimarySkill() {
        if (isPrimarySkillOnCooldown) {
            player.sendMessage(Component.text("주스킬이 아직 쿨타임 중입니다!").color(NamedTextColor.RED));
            return false;
        }
        return true;
    }

    public boolean canUseSecondarySkill() {
        if (isSecondarySkillOnCooldown) {
            player.sendMessage(Component.text("보조 스킬이 아직 쿨타임 중입니다!").color(NamedTextColor.RED));
            return false;
        }
        return true;
    }


    public String getAbilityName() {
        return abilityName;
    }

    private static class CooldownInfo {

        final int totalCooldown;
        final long endTime;
        CooldownInfo(int totalCooldown, long endTime) {
            this.totalCooldown = totalCooldown;
            this.endTime = endTime;
        }

    }

    // 이벤트 처리 메소드들
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {}
    @EventHandler
    public void onEntityDamaged(EntityDamageEvent event) {}
    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {}
    @EventHandler
    public void onHitPlayer(EntityDamageByEntityEvent event) {}
    @EventHandler
    public void onHitted(EntityDamageByEntityEvent event) {}
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {}
    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {}
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {}
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {}
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {}
    @EventHandler
    public void onMouseClick(PlayerInteractEvent event) {}
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {}
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {}
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {}
    @EventHandler
    public void onPlayerItemPickUp(PlayerPickupItemEvent event) {}
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {}
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {}
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {}
    @EventHandler
    public void onPlayerShootBow(EntityShootBowEvent event) {}
    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent event) {}

    public void onKillPlayer(Player killer) {}
    public void onKilledByEnemy(Player killer) {}
    @EventHandler
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        // 기본 구현은 비어있습니다. 필요한 경우 하위 클래스에서 오버라이드합니다.
    }
    @EventHandler
    public void onDamageReceived(EntityDamageByEntityEvent event) {
        // 기본 구현은 비어있습니다. 필요한 경우 하위 클래스에서 오버라이드합니다.
    }

    public void onKill(Player victim) {
        // 기본 구현은 비어있습니다. 필요한 경우 하위 클래스에서 오버라이드합니다.
    }
}