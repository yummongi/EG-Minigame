package kr.egsuv.minigames.ability;

import kr.egsuv.minigames.Minigame;
import kr.egsuv.minigames.MinigameState;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.List;

public class Pulutos extends Ability {

    private static final int COOLDOWN_PRIMARY = 30;

    public Pulutos(Minigame game, Player player) {
        super(game, player, "플루토스");
        cooldownPrimary = COOLDOWN_PRIMARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill() || player.isDead()) return;

        if (game.countItem(player, Material.COBBLESTONE) >= 10) {
            startCooldown(SkillType.PRIMARY);
            int cobblestoneCount = game.countItem(player, Material.COBBLESTONE);
            game.removeItem(player, Material.COBBLESTONE, cobblestoneCount);
            int originalCount = cobblestoneCount;
            int multiplier = (int) (Math.random() * 20) + 1; // 1부터 20까지의 랜덤 값
            cobblestoneCount = (cobblestoneCount * multiplier) / 10;
            player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, cobblestoneCount));
            player.sendTitle(multiplier > 10 ? "§e§l도박 성공!" : "§c§l도박 실패",
                    originalCount + " 개의 돌이 " + cobblestoneCount + "개로 " + (multiplier > 10 ? "늘어났" : "줄어들") + "습니다.",
                    10, 60, 20);
        } else {
            player.sendTitle("", "10개 이상 소지한 상태에서 사용하세요.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {

    }

    @Override
    public String getPrimaryDescription() {
        return "자신이 가진 모든 코블스톤으로 도박을 합니다. 코블스톤을 10개 이상 소지하고 있을 경우에만 가능합니다. 도박 시 0.1 ~ 2.0배의 돌을 얻습니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "패시브 스킬이 없습니다.";
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬 없음
    }

    @Override
    public void itemSupply() {
        // 기본 아이템 제공 없음
    }
}