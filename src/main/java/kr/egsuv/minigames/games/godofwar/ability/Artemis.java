package kr.egsuv.minigames.games.godofwar.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Artemis extends Ability {

    private static final int COOLDOWN_PRIMARY = 80;
    private static final int COOLDOWN_SECONDARY = 10;

    public Artemis(WarOfGodGame game, Player player) {
        super(game, player, "아르테미스");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
        this.cooldownSecondary = COOLDOWN_SECONDARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 20)) {
            startCooldown(SkillType.PRIMARY);
            player.getInventory().addItem(new ItemStack(Material.BOW, 1));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "§a돌을 활로 변환하였습니다.", 10, 60, 20);
        } else {
            player.sendTitle("", "§c돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        if (!canUseSecondarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 1)) {
            startCooldown(SkillType.SECONDARY);
            player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "§a돌을 화살로 변환하였습니다.", 10, 60, 20);
        } else {
            player.sendTitle("", "§c돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void passiveSkill() {
        // 아르테미스는 패시브 스킬이 없습니다.
    }

    @Override
    public void itemSupply() {
        // 아르테미스는 초기 아이템 지급이 없습니다.
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 20개를 사용하여 활 1개로 변환합니다. (쿨타임: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "코블스톤 1개를 사용하여 화살 1개로 변환합니다. (쿨타임: " + COOLDOWN_SECONDARY + "초)";
    }

    @Override
    public String getPassiveDescription() {
        return "패시브 스킬이 없습니다.";
    }
}