package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

public class Gaia extends Ability {

    private static final int COOLDOWN_PRIMARY = 10;

    public Gaia(WarOfGodGame game, Player player) {
        super(game, player, "가이아");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.hasItem(player, Material.COBBLESTONE, 1)) {
            startCooldown(SkillType.PRIMARY);
            for (int i = 0; i < 10; i++) {
                if (game.takeItem(player, Material.COBBLESTONE, 1)) {
                    player.getInventory().addItem(new ItemStack(Material.DIRT, 3));
                    player.sendTitle("", "돌을 흙으로 변환했습니다.", 10, 60, 20);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
                }
            }
        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        // 가이아는 보조 스킬이 없습니다.
    }

    @Override
    public void passiveSkill() {
        // 가이아는 패시브 스킬이 없습니다.
    }

    @Override
    public void itemSupply() {
        // 가이아는 초기 아이템 지급이 없습니다.
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 1개를 흙 3개로 변환합니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "낙하 피해를 받지 않습니다.";
    }

    @EventHandler
    @Override
    public void onEntityDamaged(EntityDamageEvent event) {
        if (!event.getEntity().equals(this.player)) return;
        if (event.getCause() == DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

}