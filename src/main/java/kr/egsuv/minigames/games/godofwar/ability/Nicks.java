package kr.egsuv.minigames.games.godofwar.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class Nicks extends Ability {

    private static final int COOLDOWN_PRIMARY = 80;

    public Nicks(WarOfGodGame game, Player player) {
        super(game, player, "닉스");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 12개를 소모하여 8칸 내 적을 10초간 실명상태에 빠뜨립니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "블레이즈 막대로 타격한 대상은 5초간 실명상태에 빠집니다.";
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 12)) {
            startCooldown(SkillType.PRIMARY);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0f, 0.1f);
            player.sendTitle("", "적을 밤의 세계로 이끕니다.", 10, 60, 20);

            List<Player> enemies = game.getEnemyList(game.getPlayerTeam(player));

            for (Player enemy : enemies) {
                if (enemy.getLocation().distance(player.getLocation()) <= 8) {
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0));
                    enemy.sendTitle("", "닉스에 의해 밤의 세계로 끌려갔습니다.", 10, 60, 20);
                }
            }
        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        // 보조 스킬 없음
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬 로직은 onHitPlayer 메소드에 포함되어 있습니다.
    }

    @Override
    public void itemSupply() {
        // 아이템 보급 없음
    }

    @Override
    public void onHitPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();
        ItemStack item = damager.getInventory().getItemInMainHand();
        if (item == null) return;
        if (item.getType() == Material.BLAZE_ROD) {
            Player victim = (Player) event.getEntity();
            victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.0f, 2.5f);
        }
    }
}