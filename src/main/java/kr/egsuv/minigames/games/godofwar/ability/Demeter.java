package kr.egsuv.minigames.games.godofwar.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;

public class Demeter extends Ability {

    private static final int COOLDOWN_PRIMARY = 10;
    private static final int COOLDOWN_SECONDARY = 5;

    public Demeter(WarOfGodGame game, Player player) {
        super(game, player, "데메테르");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
        this.cooldownSecondary = COOLDOWN_SECONDARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 1)) {
            startCooldown(SkillType.PRIMARY);
            player.getInventory().addItem(new ItemStack(Material.OAK_SAPLING, 1));
            player.getInventory().addItem(new ItemStack(Material.WHEAT_SEEDS, 4));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "§a돌을 묘목 1개와 씨앗 4개로 변환하였습니다.", 10, 60, 20);
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
            player.getInventory().addItem(new ItemStack(Material.BONE, 3));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "§a돌을 뼈 3개로 변환하였습니다.", 10, 60, 20);
        } else {
            player.sendTitle("", "§c돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void passiveSkill() {
        // 데메테르의 패시브 스킬: 배고픔이 닳지 않으며, 체력 회복 속도가 2배가 됩니다.
    }

    @Override
    public void itemSupply() {
        // 데메테르는 초기 아이템 지급이 없습니다.
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 1개를 묘목 1개와 씨앗 4개로 바꿉니다. (쿨타임: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "코블스톤 1개를 뼈 3개로 바꿉니다. (쿨타임: " + COOLDOWN_SECONDARY + "초)";
    }

    @Override
    public String getPassiveDescription() {
        return "배고픔이 닳지 않으며, 체력 회복 속도가 2배가 됩니다.";
    }

    @Override
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);  // 배고픔이 닳지 않게 설정
    }

    @Override
    public void onRegainHealth(EntityRegainHealthEvent event) {
        event.setAmount(event.getAmount() * 2);  // 체력 회복 속도 2배로 설정
    }
}