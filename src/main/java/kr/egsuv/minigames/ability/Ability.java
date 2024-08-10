package kr.egsuv.minigames.ability;

import kr.egsuv.EGServerMain;
import kr.egsuv.minigames.Minigame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

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


    private EGServerMain plugin = EGServerMain.getInstance();

    public Ability(Minigame game, Player player, String abilityName) {
        this.game = game;
        this.player = player;
        this.abilityName = abilityName;
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
        int cooldown = 0;
        switch (skillType) {
            case PRIMARY:
                cooldown = cooldownPrimary;
                isPrimarySkillOnCooldown = true;
                break;
            case SECONDARY:
                cooldown = cooldownSecondary;
                isSecondarySkillOnCooldown = true;
                break;
            case ITEM_KIT:
                cooldown = cooldownItemKit;
                break;
        }

        int finalCooldown = cooldown;
        new BukkitRunnable() {
            int secondsLeft = finalCooldown;

            @Override
            public void run() {
                if (secondsLeft > 0) {
                    double progress = (double) secondsLeft / finalCooldown;
                    String barColor = progress > 0.5 ? "§c" : (progress > 0.25 ? "§e" : "§a");
                    String progressBar = StringUtils.repeat("█", (int) (progress * 10)) +
                            StringUtils.repeat("░", 10 - (int) (progress * 10));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(barColor + skillType + " 쿨타임: " + progressBar + " " + secondsLeft + "초"));
                    secondsLeft--;
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText("§a" + skillType + " 사용 가능!"));
                    player.sendTitle("", "§a" + skillType + " 사용 가능!", 10, 40, 10);
                    if (skillType == SkillType.PRIMARY) {
                        isPrimarySkillOnCooldown = false;
                    } else if (skillType == SkillType.SECONDARY) {
                        isSecondarySkillOnCooldown = false;
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player.equals(this.player) && event.isSneaking()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getType() == Material.BLAZE_ROD) {
                showHelp();
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
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

    // 이벤트 처리 메소드들
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {}
    public void onEntityDamaged(EntityDamageEvent event) {}
    public void onClickInventory(InventoryClickEvent event) {}
    public void onHitPlayer(EntityDamageByEntityEvent event) {}
    public void onHitted(EntityDamageByEntityEvent event) {}
    public void onBlockBreak(BlockBreakEvent event) {}
    public void onBlockPlaced(BlockPlaceEvent event) {}
    public void onPlayerDeath(PlayerDeathEvent event) {}
    public void onPlayerRespawn(PlayerRespawnEvent event) {}
    public void onPlayerQuit(PlayerQuitEvent event) {}
    public void onMouseClick(PlayerInteractEvent event) {}
    public void onFoodLevelChange(FoodLevelChangeEvent event) {}
    public void onInventoryClose(InventoryCloseEvent event) {}
    public void onPlayerMove(PlayerMoveEvent event) {}
    public void onPlayerItemPickUp(PlayerPickupItemEvent event) {}
    public void onItemDrop(PlayerDropItemEvent event) {}
    public void onChat(AsyncPlayerChatEvent event) {}
    public void onSneak(PlayerToggleSneakEvent event) {}
    public void onPlayerShotBow(EntityShootBowEvent event) {}
    public void onRegainHealth(EntityRegainHealthEvent event) {}

    public void onKillPlayer(Player killer) {}
    public void onKilledByEnemy(Player killer) {}

    public void onDamageDealt(EntityDamageByEntityEvent event) {
        // 기본 구현은 비어있습니다. 필요한 경우 하위 클래스에서 오버라이드합니다.
    }

    public void onDamageReceived(EntityDamageByEntityEvent event) {
        // 기본 구현은 비어있습니다. 필요한 경우 하위 클래스에서 오버라이드합니다.
    }

    public void onKill(Player victim) {
        // 기본 구현은 비어있습니다. 필요한 경우 하위 클래스에서 오버라이드합니다.
    }
}