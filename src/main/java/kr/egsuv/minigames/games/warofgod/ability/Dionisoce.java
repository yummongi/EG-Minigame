package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class Dionisoce extends Ability {

    private static final Random random = new Random();

    public Dionisoce(WarOfGodGame game, Player player) {
        super(game, player, "디오니소스");
    }

    @Override
    public void primarySkill() {
        // 디오니소스는 기본 스킬이 없습니다.
    }

    @Override
    public void secondarySkill() {
        // 디오니소스는 보조 스킬이 없습니다.
    }

    @Override
    public void passiveSkill() {
        // 디오니소스는 패시브 스킬만 있습니다.
    }

    @Override
    public void itemSupply() {
        // 디오니소스는 초기 아이템 지급이 없습니다.
    }

    @Override
    public String getPrimaryDescription() {
        return "기본 스킬이 없습니다.";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "공격 시 33% 확률로 상대방에게 느려짐, 약화, 혼란 효과를 부여합니다.";
    }

    @EventHandler
    @Override
    public void onHitPlayer(EntityDamageByEntityEvent event) {
        if (!event.getDamager().equals(this.player)) return;
        if (event.getEntity() instanceof Player) {
            Player target = (Player) event.getEntity();
            if (random.nextInt(3) == 0) {  // 33% 확률
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
            }
        }
    }
}