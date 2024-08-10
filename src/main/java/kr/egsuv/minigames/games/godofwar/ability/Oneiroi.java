package kr.egsuv.minigames.games.godofwar.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Oneiroi extends Ability {

    private static final int COOLDOWN_PRIMARY = 40;
    private static final int COOLDOWN_SECONDARY = 40;

    public Oneiroi(WarOfGodGame game, Player player) {
        super(game, player, "오네이로이");
        cooldownPrimary = COOLDOWN_PRIMARY;
        cooldownSecondary = COOLDOWN_SECONDARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill() || player.isDead()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 5)) {
            startCooldown(SkillType.PRIMARY);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 300, 0));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "투명해졌습니다.", 10, 60, 20);
        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        // 보조 스킬 구현 없음
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬 없음
    }

    @Override
    public void itemSupply() {
        // 기본 아이템 제공 없음
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 5개를 사용하여 15초간 투명화됩니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "피해를 입을 때 5초간 속도 버프를 받습니다.";
    }

    @Override
    public void onEntityDamaged(EntityDamageEvent event) {
        if (event.getEntity().equals(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0));
        }
    }

    @Override
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity().equals(player)) {
            event.setAmount(event.getAmount() * 2);
        }
    }
}