package kr.egsuv.minigames.games.godofwar.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.concurrent.ThreadLocalRandom;

public class Poseidon extends Ability {

    private static final int COOLDOWN_PRIMARY = 30;

    public Poseidon(WarOfGodGame game, Player player) {
        super(game, player, "포세이돈");
        cooldownPrimary = COOLDOWN_PRIMARY;
    }

    @Override
    public void primarySkill() {
        // 주 스킬은 블레이즈 막대로 타격했을 때 발동되므로 이 메서드는 비워둡니다.
    }

    @Override
    public void secondarySkill() {
        // 보조 스킬 없음
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
        return "코블스톤 1개를 소모하여 바라보는 방향으로 타격 대상을 밀쳐냅니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "물에서 33% 확률로 모든 데미지를 회피하고 익사하지 않습니다.";
    }

    @Override
    public void onEntityDamaged(EntityDamageEvent event) {
        if (event.getEntity().equals(player)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
                event.setCancelled(true);
                return;
            }

            if (isInWater(player)) {
                if (ThreadLocalRandom.current().nextInt(100) < 33) { // 33% 확률
                    player.sendTitle("", "데미지 회피", 10, 60, 20);
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onHitPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager().equals(player) && player.getInventory().getItemInMainHand().getType() == Material.BLAZE_ROD) {
            if (canUsePrimarySkill()) {
                if (game.takeItem(player, Material.COBBLESTONE, 1)) {
                    startCooldown(SkillType.PRIMARY);
                    Player target = (Player) event.getEntity();
                    target.setVelocity(player.getEyeLocation().getDirection().multiply(2.8f));
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1.5F, 0.5F);
                    player.getWorld().spawnParticle(Particle.SPLASH, player.getEyeLocation(), 20, 0.2F, 0.2f, 0.2f, 0.1f);
                    player.sendTitle("", "파도를 일으켰습니다.", 10, 60, 20);
                    player.getLocation().add(0, 1, 0).getBlock().setType(Material.WATER);
                } else {
                    player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
                }
            }
        }
    }

    private boolean isInWater(Player player) {
        Material blockType = player.getLocation().getBlock().getType();
        Material blockAboveType = player.getLocation().add(0, 1, 0).getBlock().getType();
        return blockType == Material.WATER || blockAboveType == Material.WATER;
    }
}