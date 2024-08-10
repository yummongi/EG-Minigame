package kr.egsuv.minigames.games.godofwar.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

public class Skadi extends Ability {

    private static final int COOLDOWN_PRIMARY = 8;

    public Skadi(WarOfGodGame game, Player player) {
        super(game, player, "스카디");
        cooldownPrimary = COOLDOWN_PRIMARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill() || player.isDead()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 1)) {
            startCooldown(SkillType.PRIMARY);
            player.getInventory().addItem(new ItemStack(Material.SNOWBALL, 1));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "돌을 눈덩이로 변환하였습니다.", 10, 60, 20);
        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void onHitPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Snowball) {
            Snowball snowball = (Snowball) event.getDamager();
            if (snowball.getShooter() instanceof Player) {
                Player target = (Player) event.getEntity();
                Player shooter = (Player) snowball.getShooter();

                Location targetLocation = target.getLocation();
                target.teleport(shooter.getLocation(), TeleportCause.PLUGIN);
                shooter.teleport(targetLocation, TeleportCause.PLUGIN);

                // 효과음 및 파티클 효과 추가
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 50, 0.5, 0.5, 0.5, 0.1);
                shooter.getWorld().spawnParticle(Particle.PORTAL, shooter.getLocation(), 50, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 1개를 눈덩이 1개로 교환합니다. 이 눈덩이로 적을 맞출 시 해당 적과 자리를 변경합니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
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
    public void secondarySkill() {
        // 보조 스킬 없음
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬 없음
    }

    @Override
    public void itemSupply() {
        // 아이템 공급 없음
    }
}